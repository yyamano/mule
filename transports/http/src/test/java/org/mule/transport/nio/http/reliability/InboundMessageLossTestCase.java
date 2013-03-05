/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.reliability;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.message.DefaultExceptionPayload;
import org.mule.routing.filters.WildcardFilter;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConstants;

/**
 * Verify that no inbound messages are lost when exceptions occur. The message must
 * either make it all the way to the SEDA queue (in the case of an asynchronous
 * inbound endpoint), or be restored/rolled back at the source. In the case of the
 * HTTP transport, there is no way to restore the source message so an exception is
 * simply returned to the client.
 */
public class InboundMessageLossTestCase extends FunctionalTestCase
{
    protected HttpClient httpClient = new HttpClient();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        // Set SystemExceptionStrategy to redeliver messages (this can only be
        // configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter(
            "*"));
    }

    @Test
    public void testNoException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/noException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
        assertEquals("Here you go", request.getResponseBodyAsString());
    }

    @Test
    public void testTransformerException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/transformerException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }

    @Test
    public void testHandledTransformerException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
        assertTrue(request.getResponseBodyAsString().contains("Success!"));
    }

    @Test
    public void testNotHandledTransformerException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/notHandledTransformerException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Bad news"));
    }

    @Test
    public void testRouterException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/routerException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("Failure"));
    }

    @Test
    public void testComponentException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
        final int status = httpClient.executeMethod(request);
        // Component exception occurs after the SEDA queue for an asynchronous
        // request, but since
        // this request is synchronous, the failure propagates back to the client.
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, status);
        assertTrue(request.getResponseBodyAsString().contains("exception"));
    }

    protected HttpMethodBase createRequest(final String uri)
    {
        return new GetMethod(uri);
    }

    protected String getBaseUri()
    {
        return "http://localhost:" + dynamicPort.getNumber();
    }

    /**
     * Custom Exception Handler that handles an exception
     */
    public static class Handler extends AbstractMessagingExceptionStrategy
    {
        public Handler(final MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleEvent handleException(final Exception ex, final MuleEvent event)
        {
            doHandleException(ex, event);
            ((MessagingException)ex).setHandled(true);
            return new DefaultMuleEvent(new DefaultMuleMessage("Success!", muleContext), event);
        }
    }

    /**
     * Custom Exception Handler that creates a different exception
     */
    public static class BadHandler extends AbstractMessagingExceptionStrategy
    {
        public BadHandler(final MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        public MuleEvent handleException(final Exception ex, final MuleEvent event)
        {
            doHandleException(ex, event);
            final DefaultMuleMessage message = new DefaultMuleMessage(NullPayload.getInstance(), muleContext);
            message.setExceptionPayload(new DefaultExceptionPayload(new MessagingException(event,
                new RuntimeException("Bad news!"))));
            return new DefaultMuleEvent(message, event);
        }
    }
}

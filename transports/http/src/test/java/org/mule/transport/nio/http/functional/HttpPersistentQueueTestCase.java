/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConstants;

public class HttpPersistentQueueTestCase extends AbstractServiceAndFlowTestCase
{
    private final CountDownLatch messageDidArrive = new CountDownLatch(1);
    private int port = -1;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpPersistentQueueTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "http-persistent-queue-service.xml"},
            {ConfigVariant.FLOW, "http-persistent-queue-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent("PersistentQueueAsync");
        assertNotNull(testComponent);
        testComponent.setEventCallback(new Callback(messageDidArrive));
        port = dynamicPort.getNumber();
    }

    @Test
    public void testPersistentMessageDeliveryWithGet() throws Exception
    {
        final GetMethod method = new GetMethod("http://localhost:" + port + "/services/Echo?foo=bar");
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        doTestPersistentMessageDelivery(method);
    }

    @Test
    public void testPersistentMessageDeliveryWithPost() throws Exception
    {
        final PostMethod method = new PostMethod("http://localhost:" + port + "/services/Echo");
        method.addRequestHeader(HttpConstants.HEADER_CONNECTION, "close");
        method.addParameter(new NameValuePair("foo", "bar"));
        doTestPersistentMessageDelivery(method);
    }

    private void doTestPersistentMessageDelivery(final HttpMethod httpMethod) throws Exception
    {
        final HttpClient client = new HttpClient();
        final int rc = client.executeMethod(httpMethod);

        assertEquals(HttpStatus.SC_OK, rc);
        assertTrue(messageDidArrive.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private static class Callback implements EventCallback
    {
        private final CountDownLatch messageDidArrive;

        public Callback(final CountDownLatch latch)
        {
            super();
            messageDidArrive = latch;
        }

        public void eventReceived(final MuleEventContext context, final Object component) throws Exception
        {
            final MuleMessage message = context.getMessage();

            final Object httpMethod = message.getInboundProperty("http.method");
            if (HttpConstants.METHOD_GET.equals(httpMethod))
            {
                assertEquals("/services/Echo?foo=bar", message.getPayloadAsString());
            }
            else if (HttpConstants.METHOD_POST.equals(httpMethod))
            {
                assertEquals("foo=bar", message.getPayloadAsString());
            }
            else
            {
                fail("invalid HTTP method : " + httpMethod);
            }

            assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_CONNECTION));
            assertEquals("true", message.getInboundProperty(HttpConstants.HEADER_KEEP_ALIVE));

            messageDidArrive.countDown();
        }
    }

}

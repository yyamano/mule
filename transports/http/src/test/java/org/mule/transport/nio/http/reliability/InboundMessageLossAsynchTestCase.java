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

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.Test;
import org.mule.transport.nio.http.HttpConstants;

/**
 * Verify that no inbound messages are lost when exceptions occur. The message must
 * either make it all the way to the SEDA queue (in the case of an asynchronous
 * inbound endpoint), or be restored/rolled back at the source. In the case of the
 * HTTP transport, there is no way to restore the source message so an exception is
 * simply returned to the client.
 */
public class InboundMessageLossAsynchTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss-asynch.xml";
    }

    @Test
    @Override
    public void testNoException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/noException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
    }

    @Test
    @Override
    public void testHandledTransformerException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/handledTransformerException");
        final int status = httpClient.executeMethod(request);
        assertEquals(HttpConstants.SC_OK, status);
    }

    @Test
    @Override
    public void testComponentException() throws Exception
    {
        final HttpMethodBase request = createRequest(getBaseUri() + "/componentException");
        final int status = httpClient.executeMethod(request);
        // Component exception occurs after the SEDA queue for an asynchronous
        // request, so from the client's
        // perspective, the message has been delivered successfully.
        assertEquals(HttpConstants.SC_OK, status);
    }

    @Override
    protected HttpMethodBase createRequest(final String uri)
    {
        return new PostMethod(uri);
    }
}

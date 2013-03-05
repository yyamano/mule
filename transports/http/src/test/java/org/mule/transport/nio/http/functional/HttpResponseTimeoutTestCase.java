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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.http.functional.AbstractHttpResponseTimeoutTestCase;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;

/**
 * See MULE-4491 "Http outbound endpoint does not use responseTimeout attribute" See
 * MULE-4743 "MuleClient.send() timeout is not respected with http transport"
 */
@NioTest
public class HttpResponseTimeoutTestCase extends AbstractHttpResponseTimeoutTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);

    public HttpResponseTimeoutTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDecreaseMuleClientSendResponseTimeout() throws Exception
    {
        final Date beforeCall = new Date();
        Date afterCall;

        try
        {
            muleClient.send(
                ((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inDelayService")).getAddress(),
                getPayload(), null, 1000);
            fail("SocketTimeoutException expected");
        }
        catch (final Exception e)
        {
            assertTrue(org.apache.commons.lang.exception.ExceptionUtils.getRootCause(e) instanceof TimeoutException);
        }

        // Exception should have been thrown after timeout specified which is
        // less than default.
        afterCall = new Date();
        assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
    }
}

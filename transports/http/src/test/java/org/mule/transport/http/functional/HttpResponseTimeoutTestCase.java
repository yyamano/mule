/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.tcp.LegacyIoTest;
import org.mule.util.ExceptionUtils;

import java.net.SocketTimeoutException;
import java.util.Date;

import org.junit.Test;

@LegacyIoTest
public class HttpResponseTimeoutTestCase extends AbstractHttpResponseTimeoutTestCase
{
    /**
     * @param variant
     * @param configResources
     */
    public HttpResponseTimeoutTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDecreaseMuleClientSendResponseTimeout() throws Exception
    {
        Date beforeCall = new Date();
        Date afterCall;

        try
        {
            muleClient.send(((InboundEndpoint) muleClient.getMuleContext().getRegistry().lookupObject("inDelayService")).getAddress(), getPayload(), null, 1000);
            fail("SocketTimeoutException expected");
        }
        catch (Exception e)
        {
            assertTrue(ExceptionUtils.getRootCause(e) instanceof SocketTimeoutException);
        }

        // Exception should have been thrown after timeout specified which is
        // less than default.
        afterCall = new Date();
        assertTrue((afterCall.getTime() - beforeCall.getTime()) < DEFAULT_RESPONSE_TIMEOUT);
    }
}



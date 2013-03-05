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

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Rule;

@NioTest
public class HttpMethodTestCase extends org.mule.transport.http.functional.HttpMethodTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);

    public HttpMethodTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected String getHttpEndpointAddress()
    {
        return ((InboundEndpoint) muleContext.getRegistry().lookupObject("inHttpIn")).getAddress().replace(HttpConnector.PROTOCOL, "http");
    }
    
    @Override
    protected void validateBadMethod(int statusCode)
    {
        assertEquals(HttpStatus.SC_METHOD_NOT_ALLOWED, statusCode);
    }
}

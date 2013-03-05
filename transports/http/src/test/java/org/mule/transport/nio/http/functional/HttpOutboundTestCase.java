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

import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

import org.junit.Rule;
import org.junit.Test;

@NioTest
public class HttpOutboundTestCase extends org.mule.transport.http.functional.HttpOutboundTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);

    public HttpOutboundTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testOutboundConnect() throws Exception
    {
        sendHttpRequest("vm://doConnect", HttpConstants.METHOD_CONNECT);
    }
}

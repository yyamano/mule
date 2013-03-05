/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http;

import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

import org.junit.Rule;
import org.junit.Test;

@NioTest
public class HttpPlaceholderTestCase extends AbstractNamespaceHandlerTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    public HttpPlaceholderTestCase()
    {
        super("http");
    }

    @Override
    protected String getConfigResources()
    {
        return "http-placeholder-test.xml";
    }

    @Test
    public void testConnectorProperties()
    {
        final HttpConnector connector = (HttpConnector) muleContext.getRegistry().lookupConnector(
            "httpConnector");
        testBasicProperties(connector);
    }
}

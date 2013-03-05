/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.issues;

import org.junit.Rule;
import org.junit.Test;
import org.mule.transport.nio.http.AbstractNamespaceHandlerTestCase;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class TypedPlaceholderMule1887TestCase extends AbstractNamespaceHandlerTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    public TypedPlaceholderMule1887TestCase()
    {
        super("http");
    }

    @Override
    protected String getConfigResources()
    {
        return "typed-placeholder-mule-1887-test.xml";
    }

    @Test
    public void testConnectorProperties()
    {
        final HttpConnector connector = (HttpConnector) muleContext.getRegistry().lookupConnector(
            "httpConnector");
        testBasicProperties(connector);
    }
}

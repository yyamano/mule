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

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.transport.Connector;
import org.mule.api.transport.SessionHandler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.functional.TestSessionHandler;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class HttpServiceOverridesTestCase extends FunctionalTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Override
    protected String getConfigResources()
    {
        return "http-service-overrides.xml";
    }

    @Test
    public void testSessionHandler()
    {
        final Connector connector = muleContext.getRegistry().lookupConnector("httpConnector");
        assertTrue(connector instanceof HttpConnector);

        final HttpConnector httpConnector = (HttpConnector) connector;
        final SessionHandler sessionHandler = httpConnector.getSessionHandler();
        assertTrue(sessionHandler instanceof TestSessionHandler);
    }
}

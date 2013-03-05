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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.tck.junit4.FunctionalTestCase;

public abstract class AbstractNamespaceHandlerTestCase extends FunctionalTestCase
{
    private final String prefix;

    protected AbstractNamespaceHandlerTestCase(final String prefix)
    {
        this.prefix = prefix;
    }

    @Override
    protected String getConfigResources()
    {
        return prefix + "-namespace-config.xml";
    }

    protected void testBasicProperties(final HttpConnector connector)
    {
        assertNotNull(connector);

        assertEquals("bcd", connector.getProxyHostname());
        assertEquals("cde", connector.getProxyPassword());
        assertEquals(2345, connector.getProxyPort());
        assertEquals("def", connector.getProxyUsername());
        assertEquals(34, connector.getReceiveBacklog());
        assertEquals(4567, connector.getReceiveBufferSize());
        assertEquals(5678, connector.getSendBufferSize());
        assertEquals(6789, connector.getSocketSoLinger());
        assertEquals(true, connector.isEnableCookies());
        assertEquals(true, connector.isKeepAlive());
        assertEquals(true, connector.isKeepSendSocketOpen());
        assertEquals(true, connector.isSendTcpNoDelay());
        assertEquals(false, connector.isValidateConnections());
    }

}

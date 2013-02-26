/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.nio.tcp.protocols.CustomClassLoadingLengthProtocol;

@NioTest
public class TcpNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Override
    protected String getConfigResources()
    {
        return "nio/tcp-namespace-config.xml";
    }

    @Test
    public void testConfig() throws Exception
    {
        final TcpConnector c = lookupTcpConnector("tcpConnector");
        assertNotNull(c);
        assertEquals(1024, c.getReceiveBufferSize());
        assertEquals(2048, c.getSendBufferSize());
        assertEquals(50, c.getReceiveBacklog());
        assertFalse(c.isReuseAddress());
        assertEquals(3000, c.getSocketMaxWait());
        assertTrue(c.isKeepAlive());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testSeparateTimeouts() throws Exception
    {
        final TcpConnector c = lookupTcpConnector("separateTimeouts");
        assertNotNull(c);
        assertEquals(-1, c.getSocketMaxWait());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testTcpProtocolWithClass()
    {
        final TcpConnector connector = lookupTcpConnector("connectorWithProtocolClass");
        assertTrue(connector.getTcpProtocol() instanceof MockTcpProtocol);
    }

    @Test
    public void testTcpProtocolWithRef()
    {
        final TcpConnector connector = lookupTcpConnector("connectorWithProtocolRef");
        assertTrue(connector.getTcpProtocol() instanceof MockTcpProtocol);
    }

    private TcpConnector lookupTcpConnector(final String name)
    {
        final TcpConnector connector = (TcpConnector) muleContext.getRegistry().lookupConnector(name);
        assertNotNull(connector);
        return connector;
    }

    public static class MockTcpProtocol implements TcpProtocol
    {
        public Object read(final InputStream is) throws IOException
        {
            throw new UnsupportedOperationException("MockTcpProtocol");
        }

        public void write(final OutputStream os, final Object data) throws IOException
        {
            throw new UnsupportedOperationException("MockTcpProtocol");
        }
    }

    @Test
    public void testPollingConnector()
    {
        final PollingTcpConnector c = (PollingTcpConnector) muleContext.getRegistry().lookupConnector(
            "pollingConnector");
        assertNotNull(c);
        assertEquals(4000, c.getPollingFrequency());
        assertEquals(-1, c.getSocketMaxWait());
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testCustomClassLoadingProtocol() throws Exception
    {
        final TcpConnector c = (TcpConnector) muleContext.getRegistry().lookupConnector(
            "custom-class-loading-protocol-connector");
        assertNotNull(c);
        final CustomClassLoadingLengthProtocol protocol = (CustomClassLoadingLengthProtocol) c.getTcpProtocol();
        assertEquals(protocol.getClass(), CustomClassLoadingLengthProtocol.class);
        assertEquals(protocol.getClassLoader(), muleContext.getRegistry().get("classLoader"));
    }
}

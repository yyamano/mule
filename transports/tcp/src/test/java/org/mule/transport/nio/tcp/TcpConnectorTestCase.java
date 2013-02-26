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

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

@NioTest
public class TcpConnectorTestCase extends AbstractConnectorTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Override
    public Connector createConnector() throws Exception
    {
        final TcpConnector c = new TcpConnector(muleContext);
        c.setName("TcpConnector");
        return c;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "tcp://localhost:56801";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    @Test
    public void testProperties() throws Exception
    {
        final TcpConnector c = (TcpConnector) getConnector();
        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());
    }
}

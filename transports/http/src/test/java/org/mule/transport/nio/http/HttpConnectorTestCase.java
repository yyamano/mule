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

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.Connector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transport.AbstractConnectorTestCase;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.nio.tcp.TcpConnector;

@NioTest
public class HttpConnectorTestCase extends AbstractConnectorTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Override
    public Connector createConnector() throws Exception
    {
        final HttpConnector c = new HttpConnector(muleContext);
        c.setName("HttpConnector");
        return c;
    }

    @Override
    public String getTestEndpointURI()
    {
        return "http://localhost:60127";
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "Hello".getBytes();
    }

    @Test
    public void testValidListener() throws Exception
    {
        final Service service = getTestService("orange", Orange.class);
        final InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(
            getTestEndpointURI());

        getConnector().registerListener(endpoint, getSensingNullMessageProcessor(), service);
    }

    @Test
    public void testProperties() throws Exception
    {
        final HttpConnector c = (HttpConnector) getConnector();

        c.setSendBufferSize(1024);
        assertEquals(1024, c.getSendBufferSize());
        c.setSendBufferSize(0);
        assertEquals(TcpConnector.DEFAULT_BUFFER_SIZE, c.getSendBufferSize());

        assertEquals(c.getReceiverThreadingProfile().getMaxThreadsActive(),
            1 + muleContext.getDefaultMessageReceiverThreadingProfile().getMaxThreadsActive());

        // all kinds of timeouts are being tested in TcpConnectorTestCase
    }
}

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

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connectable;
import org.mule.transport.nio.tcp.TcpClient;
import org.mule.transport.nio.tcp.TcpClientFactory;
import org.mule.transport.nio.tcp.TcpClientKey;

/**
 * A factory that builds {@link HttpClient}s.
 */
public class HttpClientFactory extends TcpClientFactory
{
    private final HttpConnector httpConnector;

    public HttpClientFactory(final HttpConnector httpConnector)
    {
        super(httpConnector);
        this.httpConnector = httpConnector;
    }

    @Override
    protected TcpClient newTcpClient(final TcpClientKey tcpSocketKey) throws CreateException
    {
        final Connectable connectable = tcpSocketKey.getConnectable();
        final ImmutableEndpoint endpoint = tcpSocketKey.getEndpoint();

        return HttpConnector.isWebSocketEndpoint(endpoint) ? new WebSocketClient(httpConnector, connectable,
            endpoint) : new HttpClient(httpConnector, connectable, endpoint);
    }
}

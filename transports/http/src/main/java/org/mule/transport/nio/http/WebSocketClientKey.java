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

import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connectable;
import org.mule.transport.nio.tcp.TcpClientKey;
import org.mule.util.ClassUtils;

/**
 * A specialized {@link TcpClientKey} that includes the endpoint URI path in its hash
 * because it is relevant for WebSocket connections.
 */
public class WebSocketClientKey extends TcpClientKey
{
    public WebSocketClientKey(final Connectable connectable, final ImmutableEndpoint endpoint)
    {
        super(connectable, endpoint);
    }

    @Override
    protected int computeHash(final ImmutableEndpoint endpoint)
    {
        final EndpointURI endpointURI = endpoint.getEndpointURI();
        return ClassUtils.hash(new Object[]{endpointURI.getScheme(), endpointURI.getHost(),
            endpointURI.getPort(), endpointURI.getPath()});
    }
}

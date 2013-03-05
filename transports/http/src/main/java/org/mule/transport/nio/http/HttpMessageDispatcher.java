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

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.transport.nio.tcp.TcpMessageDispatcher;

/**
 * <code>HttpMessageDispatcher</code> dispatches Mule events over HTTP.
 */
public class HttpMessageDispatcher extends TcpMessageDispatcher
{
    public HttpMessageDispatcher(final OutboundEndpoint endpoint) throws CreateException
    {
        super(endpoint);
    }

    @Override
    protected void applyOutboundTransformers(final MuleEvent event) throws MuleException
    {
        // disable HTTP outbound transformation for WS endpoints
        if (HttpConnector.isWebSocketEndpoint(endpoint))
        {
            return;
        }

        super.applyOutboundTransformers(event);
    }

    @Override
    protected void applyResponseTransformers(final MuleEvent event) throws MuleException
    {
        // disable HTTP response transformation for WS endpoints
        if (HttpConnector.isWebSocketEndpoint(endpoint))
        {
            return;
        }

        super.applyResponseTransformers(event);
    }
}

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

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.transport.nio.tcp.TcpMessageRequester;

/**
 * Requests Mule events over HTTP.
 */
public class HttpMessageRequester extends TcpMessageRequester
{
    public HttpMessageRequester(final InboundEndpoint endpoint) throws CreateException
    {
        super(endpoint);
    }

    @Override
    protected void applyInboundTransformers(final MuleMessage message) throws MuleException
    {
        // disable HTTP inbound transformation for WS endpoints
        if (HttpConnector.isWebSocketEndpoint(endpoint))
        {
            return;
        }

        super.applyInboundTransformers(message);
    }
}

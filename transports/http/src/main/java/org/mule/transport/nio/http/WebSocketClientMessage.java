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

import java.net.URISyntaxException;

import org.apache.commons.lang.Validate;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;

/**
 * Allow {@link WebSocketFrame} to be received by the Mule NIO infrastructure.
 */
public class WebSocketClientMessage extends AbstractWebSocketMessage
{
    public WebSocketClientMessage(final WebSocketFrame webSocketFrame, final WebSocketContext webSocketContext)
        throws URISyntaxException
    {
        super(webSocketFrame, webSocketContext);
    }

    @Override
    protected String extractPath() throws URISyntaxException
    {
        Validate.notNull(webSocketContext.getClientHandshaker(),
            "webSocketContext.getServerHandshaker() can't be null");

        return webSocketContext.getClientHandshaker().getWebSocketUrl().getPath();
    }

    @Override
    public WebSocketVersion getWebSocketVersion()
    {
        return webSocketContext.getClientHandshaker().getVersion();
    }
}

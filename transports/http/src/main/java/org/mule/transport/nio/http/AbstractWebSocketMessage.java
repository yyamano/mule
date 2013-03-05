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
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.jboss.netty.util.CharsetUtil;

/**
 * Provides common functionalities for {@link WebSocketServerMessage} and
 * {@link WebSocketClientMessage}.
 */
public abstract class AbstractWebSocketMessage implements WebSocketMessage
{
    protected final WebSocketFrame webSocketFrame;
    protected final WebSocketContext webSocketContext;
    protected final String path;

    public AbstractWebSocketMessage(final WebSocketFrame webSocketFrame,
                                    final WebSocketContext webSocketContext) throws URISyntaxException
    {
        Validate.notNull(webSocketFrame, "webSocketFrame can't be null");
        Validate.notNull(webSocketContext, "webSocketContext can't be null");

        this.webSocketFrame = webSocketFrame;
        this.webSocketContext = webSocketContext;

        path = extractPath();
    }

    protected abstract String extractPath() throws URISyntaxException;

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public Channel getChannel()
    {
        return webSocketContext.getChannel();
    }

    public abstract WebSocketVersion getWebSocketVersion();

    /**
     * The HTTP request path, not the logical websocket group.
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @return false to ensure that a new {@link WebSocketServerMessage} will be created
     *         for each incoming websocket frame.
     */
    public boolean isActive()
    {
        return false;
    }

    public String getEncoding()
    {
        return CharsetUtil.UTF_8.toString(); // per spec
    }

    public Object getPayload()
    {
        if (webSocketFrame instanceof TextWebSocketFrame)
        {
            return ((TextWebSocketFrame) webSocketFrame).getText();
        }
        else if (webSocketFrame instanceof ContinuationWebSocketFrame)
        {
            final ContinuationWebSocketFrame cwsf = (ContinuationWebSocketFrame) webSocketFrame;
            if (cwsf.getAggregatedText() != null)
            {
                return cwsf.getAggregatedText();
            }
            else
            {
                return getPayloadAsBytes();
            }
        }
        else
        {
            return getPayloadAsBytes();
        }
    }

    protected byte[] getPayloadAsBytes()
    {
        final ChannelBuffer binaryData = webSocketFrame.getBinaryData();
        return binaryData.readBytes(binaryData.readableBytes()).array();
    }
}

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

import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.MessageReceiver;
import org.mule.transport.nio.http.i18n.HttpMessages;

import javax.resource.spi.work.Work;

import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

public class WebSocketRequestDispatcherWork implements Work
{
    private HttpMessageReceiver receiver;
    private WorkManager workManager;
    private WebSocketServerMessage webSocketMessage;
    
    public WebSocketRequestDispatcherWork(final HttpMessageReceiver receiver,
                                          final WorkManager workManager,
                                          final WebSocketServerMessage webSocketMessage
                                          )
    {
        this.receiver = receiver;
        this.workManager = workManager;
        this.webSocketMessage = webSocketMessage;
    }

    @Override
    public void run()
    {
        try
        {
            ImmutableEndpoint endpoint = receiver.getEndpoint();
            final MuleMessage message = receiver.createMuleMessage(webSocketMessage, endpoint.getEncoding());
            final MessageReceiver actualReceiver = receiver.getTargetReceiver(message, endpoint);
    
            if (actualReceiver != null)
            {
                WebSocketMessageProcessTemplate template = new WebSocketMessageProcessTemplate((HttpMessageReceiver)actualReceiver, workManager, webSocketMessage);
                receiver.processMessage(template);
            }
            else
            {
                final String reason = HttpMessages.cannotBindToAddress(endpoint.getAddress()).toString();
                final CloseWebSocketFrame closeWebSocketFrame = new CloseWebSocketFrame(
                    HttpConstants.WS_POLICY_VIOLATION, reason);
                webSocketMessage.getChannel().write(closeWebSocketFrame);
                return;
            }
        }
        catch (final Exception e)
        {
            receiver.httpConnector.getMuleContext().getExceptionListener().handleException(e);
        }
    }

    @Override
    public void release()
    {
    }
}



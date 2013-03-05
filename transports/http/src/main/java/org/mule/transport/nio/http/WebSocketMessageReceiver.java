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

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.AbstractMessageReceiver;

/**
 * Provides support for listening to remote websockets with Mule inbound enpoints.
 */
public class WebSocketMessageReceiver extends AbstractMessageReceiver
{
    protected static final class WebSocketClientListener extends WebSocketClient
    {
        private final WebSocketMessageReceiver receiver;

        public WebSocketClientListener(final HttpConnector httpConnector,
                                       final WebSocketMessageReceiver webSocketMessageReceiver)
            throws CreateException
        {
            super(httpConnector, webSocketMessageReceiver, webSocketMessageReceiver.endpoint);
            this.receiver = webSocketMessageReceiver;
        }

        @Override
        protected void handleCloseWebSocketFrame() throws Exception
        {
            receiver.disconnect();
        }

        @Override
        protected void deliverWebSocketMessage(final WebSocketClientMessage webSocketMessage)
            throws MuleException
        {
            final MuleMessage message = receiver.createMuleMessage(webSocketMessage,
                getEndpoint().getEncoding());

            MuleEvent returnEvent = null;
            try
            {
                final ExecutionTemplate<MuleEvent> executionTemplate = receiver.createExecutionTemplate();
                returnEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
                {
                    public MuleEvent process() throws Exception
                    {
                        return receiver.routeMessage(message);
                    }
                });

                httpConnector.writeToWebSocket(returnEvent, webSocketMessage.getChannel());
            }
            catch (final Exception e)
            {
                throw new MessagingException(
                    CoreMessages.eventProcessingFailedFor(receiver.getReceiverKey()), returnEvent, e);
            }
        }
    }

    private final WebSocketClient webSocketClient;

    public WebSocketMessageReceiver(final Connector connector,
                                    final FlowConstruct flowConstruct,
                                    final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);

        webSocketClient = new WebSocketClientListener((HttpConnector) connector, this);
    }

    @Override
    protected void doConnect() throws Exception
    {
        webSocketClient.connect();
        webSocketClient.setUp();
        super.doConnect();
    }

    @Override
    protected void doDisconnect() throws Exception
    {
        webSocketClient.disconnect();
        super.doDisconnect();
    }

    @Override
    protected void applyResponseTransformers(final MuleEvent event) throws MuleException
    {
        // disable HTTP response transformation for WS endpoints
        return;
    }
}

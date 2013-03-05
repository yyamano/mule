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

import java.util.Collections;
import java.util.concurrent.TimeoutException;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ReceiveException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.util.MapUtils;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

/**
 * Will poll an http URL and use the response as the input for a service request.
 */
public class PollingHttpMessageReceiver extends AbstractPollingMessageReceiver
{
    protected final HttpConnector pollingHttpConnector;

    protected String etag = null;
    protected boolean checkEtag;
    protected boolean discardEmptyContent;

    // used for plain HTTP polling (not websocket)
    protected OutboundEndpoint outboundHttpEndpoint;

    public PollingHttpMessageReceiver(final Connector connector,
                                      final FlowConstruct flowConstruct,
                                      final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        pollingHttpConnector = setupFromConnector(connector);
    }

    protected HttpConnector setupFromConnector(final Connector connector) throws CreateException
    {
        if (!(connector instanceof HttpPollingConnector))
        {
            throw new CreateException(HttpMessages.pollingReceiverCannotbeUsed(), this);
        }

        final HttpPollingConnector pollingConnector = (HttpPollingConnector) connector;
        final long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
            pollingConnector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            setFrequency(pollingFrequency);
        }

        checkEtag = MapUtils.getBooleanValue(endpoint.getProperties(), "checkEtag",
            pollingConnector.isCheckEtag());
        discardEmptyContent = MapUtils.getBooleanValue(endpoint.getProperties(), "discardEmptyContent",
            pollingConnector.isDiscardEmptyContent());

        return (HttpPollingConnector) connector;
    }

    @Override
    public void poll() throws Exception
    {
        final ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        executionTemplate.execute(new ExecutionCallback<MuleEvent>()
        {
            public MuleEvent process() throws Exception
            {
                if (HttpConnector.isWebSocketEndpoint(endpoint))
                {
                    webSocketPoll();
                }
                else
                {
                    httpPoll();
                }
                return null;
            }
        });
    }

    protected void webSocketPoll() throws Exception
    {
        final WebSocketClient webSocketClient = (WebSocketClient) pollingHttpConnector.borrowTcpClient(this,
            endpoint);

        try
        {
            final MuleMessage polled = webSocketClient.request(getFrequency());
            if (polled == null)
            {
                logger.warn(HttpMessages.pollerReceivedANullResponse(null));
                return;
            }

            this.routeMessage(polled);
            if (logger.isDebugEnabled())
            {
                logger.debug("Routing new message: " + polled);
            }
        }
        catch (final ReceiveException re)
        {
            if (re.getCause() instanceof TimeoutException)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Socket timed out normally while doing a synchronous receive on endpointUri: "
                                 + endpoint.getEndpointURI());
                }
            }
            else
            {
                throw re;
            }
        }
    }

    protected void httpPoll() throws MuleException
    {
        final MuleContext muleContext = connector.getMuleContext();

        if (outboundHttpEndpoint == null)
        {
            // We need to create an outbound endpoint to do the polled request
            // using send() as thats the only way we can customize headers and
            // use eTags
            final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(endpoint);
            // Must not use inbound endpoint processors
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor> emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor> emptyList());
            endpointBuilder.setMessageProcessors(Collections.<MessageProcessor> emptyList());
            endpointBuilder.setResponseMessageProcessors(Collections.<MessageProcessor> emptyList());
            endpointBuilder.setExchangePattern(MessageExchangePattern.REQUEST_RESPONSE);

            outboundHttpEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint(endpointBuilder);
        }

        @SuppressWarnings("unchecked")
        final MuleMessage request = new DefaultMuleMessage(StringUtils.EMPTY,
            outboundHttpEndpoint.getProperties(), muleContext);

        if (etag != null && checkEtag)
        {
            request.setOutboundProperty(HttpConstants.HEADER_IF_NONE_MATCH, etag);
        }
        request.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, "GET");

        final MuleEvent event = new DefaultMuleEvent(request, outboundHttpEndpoint.getExchangePattern(),
            flowConstruct);

        final MuleEvent result = outboundHttpEndpoint.process(event);
        if (result == null)
        {
            logger.warn(HttpMessages.pollerReceivedANullResponse(event));
            return;
        }

        final MuleMessage message = result.getMessage();
        final int contentLength = message.getOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, -1);
        if (contentLength == 0 && discardEmptyContent)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Received empty message and ignoring from: " + endpoint.getEndpointURI());
            }
            return;
        }
        final int status = NumberUtils.toInt(message.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,
            0));
        etag = message.getOutboundProperty(HttpConstants.HEADER_ETAG);

        if (isConnected() && (status != HttpConstants.SC_NOT_MODIFIED || !checkEtag))
        {
            routeMessage(message);
        }
    }

}

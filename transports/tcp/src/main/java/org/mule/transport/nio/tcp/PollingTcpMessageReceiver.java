/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp;

import java.util.concurrent.TimeoutException;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ReceiveException;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.nio.tcp.i18n.TcpMessages;
import org.mule.util.MapUtils;

/**
 * <code>PollingTcpMessageReceiver</code> acts like a TCP client polling for new
 * messages.
 */
public class PollingTcpMessageReceiver extends AbstractPollingMessageReceiver
{
    private final PollingTcpConnector pollingTcpConnector;

    public PollingTcpMessageReceiver(final Connector connector,
                                     final FlowConstruct flowConstruct,
                                     final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);

        if (connector instanceof PollingTcpConnector)
        {
            pollingTcpConnector = (PollingTcpConnector) connector;
        }
        else
        {
            throw new CreateException(TcpMessages.pollingReceiverCannotbeUsed(), this);
        }

        final long pollingFrequency = MapUtils.getLongValue(endpoint.getProperties(), "pollingFrequency",
            pollingTcpConnector.getPollingFrequency());
        if (pollingFrequency > 0)
        {
            this.setFrequency(pollingFrequency);
        }
    }

    @Override
    public void poll() throws Exception
    {
        final ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
        executionTemplate.execute(new ExecutionCallback<MuleEvent>()
        {
            public MuleEvent process() throws Exception
            {
                doPoll();
                return null;
            }
        });
    }

    private void doPoll() throws Exception
    {
        final TcpClient tcpClient = pollingTcpConnector.borrowTcpClient(this, endpoint);

        try
        {
            final MuleMessage polled = tcpClient.request(pollingTcpConnector.getTimeout());
            if (polled != null)
            {
                this.routeMessage(polled);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Routing new message: " + polled);
                }
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
}

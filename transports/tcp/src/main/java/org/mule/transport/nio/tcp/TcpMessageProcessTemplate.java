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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.message.processing.EndPhaseTemplate;
import org.mule.message.processing.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.message.processing.ThrottlingPhaseTemplate;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.transport.nio.tcp.protocols.StreamingProtocol;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

public class TcpMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<TcpMessageReceiver, TcpConnector> implements ThrottlingPhaseTemplate, EndPhaseTemplate, RequestResponseFlowProcessingPhaseTemplate
{
    protected final ChannelInputStream channelInputStream;
    
    protected volatile boolean isFinished = false;
    protected boolean moreMessages = true;

    public TcpMessageProcessTemplate(TcpMessageReceiver messageReceiver, WorkManager flowExecutionWorkManager, ChannelInputStream channelInputStream)
    {
        super(messageReceiver, flowExecutionWorkManager);
        this.channelInputStream = channelInputStream;
    }

    @Override
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException
    {
        // should send back only if remote synch is set or no outbound endpoints
        if (getMessageReceiver().getEndpoint().getExchangePattern().hasResponse())
        {
            final Object o = muleEvent.getMessage();
            final Channel channel = channelInputStream.getChannel();
            if (!channel.isOpen())
            {
                logger.warn("Discarded response " + o
                            + " that can't be delivered to closed channel: " + channel);
            }
            try
            {
                ChannelFuture channelFuture = getMessageReceiver().tcpConnector.write(o, channel);
                channelFuture.await();
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new DefaultMuleException(e);
            }
        }
    }

    @Override
    public void messageProcessingEnded()
    {

        synchronized(this)
        {
            isFinished = true;
            this.notify();
        }
    }

    public void awaitTermination() throws InterruptedException
    {
        if (!isFinished)
        {
            synchronized(this)
            {
                if (!isFinished)
                {
                    this.wait();
                }
            }
        }
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            String throttlingExceededMessage = "API calls exceeded";
            getMessageReceiver().tcpConnector.write(throttlingExceededMessage, channelInputStream.getChannel());
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
    }
    
    @Override
    protected OutputStream getOutputStream()
    {
        return TcpConnector.getOutputStream(channelInputStream.getChannel());
    }

    @Override
    public Object acquireMessage() throws MuleException
    {
        Object readMsg = null;

        if (channelInputStream.isOpen())
        {
            TcpProtocol protocol = getMessageReceiver().tcpConnector.getTcpProtocol();
            try
            {
                readMsg = protocol.read(channelInputStream);
                
                if (!(protocol instanceof StreamingProtocol))
                {
                    // get ready for potentially more data
                    channelInputStream.resetExpectedBytes();
                }
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
        if (readMsg == null)
        {
            noMoreMessages();
        }
        return readMsg;
    }
    
    private void noMoreMessages()
    {
        isFinished = true;
        channelInputStream.deactivate();
    }
    
    @Override
    public boolean validateMessage()
    {
        try
        {
            return getOriginalMessage() != null;
        }
        catch (MuleException e)
        {
            // ignore
            return false;
        }
    }

    @Override
    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod,
                                              long maximumRequestAllowedPerPeriod,
                                              long timeUntilNextPeriodInMillis)
    {
        // ignore since we don't can't do anything with this information in TCP.
    }

}



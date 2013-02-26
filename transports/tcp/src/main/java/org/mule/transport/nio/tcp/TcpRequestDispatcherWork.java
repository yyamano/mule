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

import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.message.processing.MessageProcessContext;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.transport.nio.tcp.protocols.StreamingProtocol;
import org.mule.util.monitor.Expirable;

import java.util.concurrent.TimeUnit;

import javax.resource.spi.work.Work;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TcpRequestDispatcherWork implements Runnable, Expirable, Work
{
    private static Log logger = LogFactory.getLog(TcpRequestDispatcherWork.class);
        
    private ChannelReceiverResource channelReceiverResource;
    private TcpMessageReceiver tcpReceiver;
    private volatile boolean running = true;
    private WorkManager workManager;
    
    protected TcpRequestDispatcherWork(TcpMessageReceiver tcpReceiver, WorkManager workManager, ChannelReceiverResource channelReceiverResource)
    {
        this.tcpReceiver = tcpReceiver;
        this.workManager = workManager;
        this.channelReceiverResource = channelReceiverResource;

//        channelReceiverResource.setBeforeCloseAction(new Runnable()
//        {
//            public void run()
//            {
//                // Don't actually close the stream, we just want to know if the
//                // we want to stop receiving messages on this channel.
//                // The Protocol is responsible for closing this.
//                running = false;
//            }
//        });
    }

    @Override
    public void run()
    {
        try
        {
            do
            {
                final long keepAliveTimeout = tcpReceiver.tcpConnector.getKeepAliveTimeout();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Getting next message with keepAliveTimeout=" + keepAliveTimeout);
                }

                try
                {
                    // Create a monitor if expires was set
                    if (keepAliveTimeout > 0)
                    {
                        tcpReceiver.tcpConnector.getKeepAliveMonitor().addExpirable(keepAliveTimeout,
                            TimeUnit.MILLISECONDS, this);
                    }
                    
                    processMessage();

                    if (tcpReceiver.tcpConnector.getTcpProtocol() instanceof StreamingProtocol)
                    {
                        break;
                    }
                }
                finally
                {
                    if (keepAliveTimeout > 0)
                    {
                        tcpReceiver.tcpConnector.getKeepAliveMonitor().removeExpirable(this);
                    }
                }
            }
            while (running && channelReceiverResource.isActive());
        }
        catch (Exception e)
        {
            tcpReceiver.tcpConnector.getMuleContext().getExceptionListener().handleException(e);
        }
        finally
        {
            close();
        } 
    }
        
    private void close()
    {
        running = false;
        //no op
//            logger.debug("Closing HTTP connection.");
//            if (httpServerConnection != null && httpServerConnection.isOpen())
//            {
//                httpServerConnection.close();
//                httpServerConnection = null;
//            }
    }
    
    protected void processMessage() throws MuleException, InterruptedException
    {
        TcpMessageProcessTemplate messageContext = (TcpMessageProcessTemplate) createMessageContext();
        tcpReceiver.processMessage(messageContext);
        messageContext.awaitTermination();
    }

    private MessageProcessContext createMessageContext()
    {
        return new TcpMessageProcessTemplate(tcpReceiver, workManager, (ChannelInputStream)channelReceiverResource
//            , new Runnable()
//                {
//                    public void run()
//                    {
//                        if (!tcpReceiver.tcpConnector.isKeepSendSocketOpen())
//                        {
//                            // Don't actually close the stream, we just want to know if the
//                            // we want to stop receiving messages on this channel.
//                            // The Protocol is responsible for closing this.
//                            running = false;
//                        }
//                    }
//                }
                );
    }

    @Override
    public void expired()
    {
        close();
    }

    @Override
    public void release()
    {
        close();
    }
    
    protected TcpMessageReceiver getMessageReceiver()
    {
        return tcpReceiver;
    }
    
    protected WorkManager getWorkManager()
    {
        return workManager;
    }
    
    protected ChannelReceiverResource getChannelReceiverResource()
    {
        return channelReceiverResource;
    }
}



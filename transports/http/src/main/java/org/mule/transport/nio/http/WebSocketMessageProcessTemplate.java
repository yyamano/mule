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

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.message.processing.EndPhaseTemplate;
import org.mule.message.processing.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.message.processing.ThrottlingPhaseTemplate;
import org.mule.transport.AbstractTransportMessageProcessTemplate;

import java.io.IOException;

public class WebSocketMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector> 
        implements ThrottlingPhaseTemplate, EndPhaseTemplate, RequestResponseFlowProcessingPhaseTemplate
{
    protected final WebSocketServerMessage webSocketServerMessage;
    
    protected boolean running = true;
    protected boolean moreMessages = true;

    public WebSocketMessageProcessTemplate(HttpMessageReceiver messageReceiver,
                                          WorkManager workManager,
                                          WebSocketServerMessage webSocketServerMessage)
    {
        super(messageReceiver, workManager);
        this.webSocketServerMessage = webSocketServerMessage;
    }
    

    @Override
    public void sendResponseToClient(MuleEvent muleEvent) throws MuleException
    {
        try
        {
            getMessageReceiver().httpConnector.writeToWebSocket(muleEvent, webSocketServerMessage.getChannel());
        }
        catch (MuleException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void messageProcessingEnded()
    {
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            String throttlingExceededMessage = "API calls exceeded";
            getMessageReceiver().httpConnector.write(throttlingExceededMessage, webSocketServerMessage.getChannel());
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod,
                                              long maximumRequestAllowedPerPeriod,
                                              long timeUntilNextPeriodInMillis)
    {
    }
    
    @Override
    public Object acquireMessage() throws MuleException
    {
        return webSocketServerMessage;
    }

}



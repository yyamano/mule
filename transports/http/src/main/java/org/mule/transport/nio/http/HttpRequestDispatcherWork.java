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
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.transport.nio.tcp.TcpRequestDispatcherWork;

import java.io.IOException;

public class HttpRequestDispatcherWork extends TcpRequestDispatcherWork
{

    HttpRequestDispatcherWork(HttpMessageReceiver receiver,
                              WorkManager workManager,
                              StreamableHttpRequest channelReceiverResource)
    {
        super(receiver, workManager, channelReceiverResource);
    }
    
    protected void processMessage() throws MuleException, InterruptedException
    {
        HttpMessageReceiver receiver = (HttpMessageReceiver)getMessageReceiver();
        StreamableHttpRequest crr = (StreamableHttpRequest) getChannelReceiverResource();

        // create message
        InboundEndpoint endpoint = receiver.getEndpoint();
        MuleMessage message = receiver.createMuleMessage(crr, endpoint.getEncoding());

        // find the actual receiver (the one for the requested path)
        HttpMessageReceiver actualReceiver = (HttpMessageReceiver) receiver.getTargetReceiver(message, receiver.getEndpoint());
        
        if (actualReceiver != null)
        {
            HttpMessageProcessTemplate messageContext = createMessageContext(actualReceiver, crr, message);
            actualReceiver.processMessage(messageContext);
            messageContext.awaitTermination();
        }
        else
        {
            // send a failure response
            HttpMessageProcessTemplate messageContext = createMessageContext(receiver, crr, message);
            String requestUri = (String) message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY);
            try
            {
                messageContext.sendFailureResponseToClient(null, HttpConstants.SC_NOT_FOUND, HttpMessages.cannotBindToAddress(requestUri).toString());
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }
    
    private HttpMessageProcessTemplate createMessageContext(HttpMessageReceiver receiver, StreamableHttpRequest crr, MuleMessage message) throws MuleException
    {
        return new HttpMessageProcessTemplate(receiver, getWorkManager(), crr, message);
    }
}



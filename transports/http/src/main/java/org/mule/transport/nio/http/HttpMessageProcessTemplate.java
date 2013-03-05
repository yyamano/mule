/*
 * $Id: HttpsHandshakeTimingTestCase.java 25119 2012-12-10 21:20:57Z pablo.lagreca $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.nio.http;

import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.transport.PropertyScope;
import org.mule.config.ExceptionHelper;
import org.mule.message.processing.EndPhaseTemplate;
import org.mule.message.processing.RequestResponseFlowProcessingPhaseTemplate;
import org.mule.message.processing.ThrottlingPhaseTemplate;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class HttpMessageProcessTemplate extends AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector> implements RequestResponseFlowProcessingPhaseTemplate, ThrottlingPhaseTemplate, EndPhaseTemplate
{

    public static final int MESSAGE_DISCARD_STATUS_CODE = Integer.valueOf(System.getProperty("mule.transport.http.throttling.discardstatuscode","429"));
    public static final String X_RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    public static final String X_RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    public static final String X_RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private StreamableHttpMessage streamableHttpMessage;
    private boolean badRequest;
    private Latch messageProcessedLatch = new Latch();
    private boolean failureSendingResponse;
    private Long remainingRequestInCurrentPeriod;
    private Long maximumRequestAllowedPerPeriod;
    private Long timeUntilNextPeriodInMillis;
    private MuleMessage message;

    public HttpMessageProcessTemplate(final HttpMessageReceiver messageReceiver, final WorkManager flowExecutionWorkManager, final StreamableHttpMessage streamableHttpMessage, MuleMessage message)
    {
        super(messageReceiver, flowExecutionWorkManager);
        this.streamableHttpMessage = streamableHttpMessage;
        this.message = message;
    }

    @Override
    public void afterFailureProcessingFlow(MessagingException messagingException) throws MuleException
    {
        if (!failureSendingResponse)
        {
            Exception e = messagingException;
            MuleEvent response = messagingException.getEvent();
            if (response != null &&
                response.getMessage().getExceptionPayload() != null &&
                response.getMessage().getExceptionPayload().getException() instanceof MessagingException)
            {
                e = (Exception) response.getMessage().getExceptionPayload().getException();
            }

            String temp = ExceptionHelper.getErrorMapping(getInboundEndpoint().getConnector().getProtocol(), messagingException.getClass(), getMuleContext());
            int httpStatus = Integer.valueOf(temp);
            try
            {
                if (e instanceof  MessagingException)
                {
                    httpStatus = response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY) != null ? Integer.valueOf(response.getMessage().getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY).toString()) : httpStatus;
                    sendFailureResponseToClient(response, httpStatus, e.getMessage());
                }
                else
                {
                    sendFailureResponseToClient(response, httpStatus, e.getMessage());
                }
            }
            catch (IOException ioException)
            {
                throw new DefaultMuleException(ioException);
            }
        }
    }

    @Override
    public void afterFailureProcessingFlow(MuleException exception) throws MuleException
    {
        if (!failureSendingResponse)
        {
            String temp = ExceptionHelper.getErrorMapping(getConnector().getProtocol(), exception.getClass(), getMuleContext());
            int httpStatus = Integer.valueOf(temp);
            try
            {
                sendFailureResponseToClient(null, httpStatus, exception.getMessage());
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }

    @Override
    public void sendResponseToClient(MuleEvent responseMuleEvent) throws MuleException
    {
        try
        {
            if (logger.isTraceEnabled())
            {
                logger.trace("Sending http response");
            }
            MuleMessage returnMessage = responseMuleEvent == null ? null : responseMuleEvent.getMessage();

            Object tempResponse;
            if (returnMessage != null)
            {
                tempResponse = returnMessage.getPayload();
            }
            else
            {
                tempResponse = NullPayload.getInstance();
            }
            // This removes the need for users to explicitly adding the response transformer
            // ObjectToHttpResponse in their config
            HttpResponse response;
            if (tempResponse instanceof HttpResponse)
            {
                response = (HttpResponse) tempResponse;
            }
            else
            {
                response = transformResponse(returnMessage);
            }
            
            String keepAliveConfigStr = (String) getMessageReceiver().getEndpoint().getProperty("keepAlive");
            Boolean keepAliveConfig = keepAliveConfigStr != null ? Boolean.parseBoolean(keepAliveConfigStr) : null;
            final boolean shouldCloseChannel = HttpUtils.shouldClose(response, keepAliveConfig);
            HttpUtils.setKeepAlive(response, !shouldCloseChannel);

            Channel channel = streamableHttpMessage.getChannel();
            HttpConnector httpConnector = (HttpConnector) getMessageReceiver().getEndpoint().getConnector();
            
            ChannelFuture channelFuture = httpConnector.write(responseMuleEvent, response, channel);
            
            
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Scheduled writing of %s on HTTP channel %s (should close? %s)",
                    response, channel, shouldCloseChannel));
            }

            if (shouldCloseChannel)
            {
                channelFuture.addListener(ChannelFutureListener.CLOSE);
            }

            if (logger.isDebugEnabled())
            {
                final HttpResponse finalResponse = response;
                channelFuture.addListener(new ChannelFutureListener()
                {
                    public void operationComplete(final ChannelFuture future) throws Exception
                    {
                        String logMessage = (future.isSuccess() ? "Successfully wrote" : "Failed to write") + " %s on channel HTTP %s (should close? %s)";
                        logger.debug(String.format(logMessage,
                            finalResponse, future.getChannel(), shouldCloseChannel));
                    }
                });
            }
        }
        catch (final IOException ioe)
        {
            failureSendingResponse = true;
            throw new MessagingException(HttpMessages.failedToWriteChunkedPayload(), responseMuleEvent, ioe);
        }
        catch (Exception e)
        {
            failureSendingResponse = true;
            if (logger.isDebugEnabled())
            {
                logger.debug("Exception while sending http response");
                logger.debug(e);
            }
            throw new MessagingException(responseMuleEvent,e);
        }
    }

    protected HttpResponse transformResponse(Object response) throws MuleException
    {
        MuleMessage message;
        if (response instanceof MuleMessage)
        {
            message = (MuleMessage) response;
        }
        else
        {
            message = new DefaultMuleMessage(response, getMessageReceiver().getEndpoint().getConnector().getMuleContext());
        }
        //TODO RM*: Maybe we can have a generic Transformer wrapper rather that using DefaultMuleMessage (or another static utility
        //class
        message.applyTransformers(null, getMessageReceiver().getResponseTransportTransformers(), HttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    protected MuleMessage createMessageFromSource(Object message) throws MuleException
    {
        MuleMessage muleMessage = new DefaultMuleMessage(this.message);
 
        warnIfMuleClientSendUsed(muleMessage);
        propagateRootMessageIdProperty(muleMessage);
        
        if (logger.isDebugEnabled())
        {
            logger.debug(muleMessage.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
        }

        // the response only needs to be transformed explicitly if
        // A) the request was not served or B) a null result was returned
        String contextPath = HttpConnector.normalizeUrl(getInboundEndpoint().getEndpointURI().getPath());
        muleMessage.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY,
                            contextPath,
                            PropertyScope.INBOUND);

        muleMessage.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY,
                                getInboundEndpoint().getEndpointURI().getAddress(),
                            PropertyScope.INBOUND);

        final String path = (String) muleMessage.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
        muleMessage.setProperty(HttpConnector.HTTP_RELATIVE_PATH_PROPERTY,
                            processRelativePath(contextPath, path),
                            PropertyScope.INBOUND);

        return muleMessage;
    }

    protected String processRelativePath(String contextPath, String path)
    {
        String relativePath = path.substring(contextPath.length());
        if (relativePath.startsWith("/"))
        {
            return relativePath.substring(1);
        }
        return relativePath;
    }

    @Override
    public Object acquireMessage() throws MuleException
    {
        return message;
    }

    public boolean validateMessage()
    {
        if (streamableHttpMessage instanceof StreamableHttpRequest)
        {
            StreamableHttpRequest request = (StreamableHttpRequest)streamableHttpMessage;
            HttpMethod method = request.getMethod();
            if (method == null)
            {
                badRequest = true;
                return false;
            }
        }
        return true;
    }

    @Override
    public void discardInvalidMessage() throws MuleException
    {
        if (badRequest)
        {
            String body = HttpMessages.malformedSyntax().toString() + HttpConstants.CRLF;
            try
            {
                sendFailureResponseToClient(null, HttpConstants.SC_BAD_REQUEST, body);
            }
            catch (IOException e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }


    @Override
    public boolean supportsAsynchronousProcessing()
    {
        return true;
    }

    public Latch getMessageProcessedLatch()
    {
        return messageProcessedLatch;
    }

    @Override
    public void discardMessageOnThrottlingExceeded() throws MuleException
    {
        try
        {
            sendFailureResponseToClient(null, MESSAGE_DISCARD_STATUS_CODE, "API calls exceeded");
        }
        catch (IOException e)
        {
            throw new DefaultMuleException(e);
        }
    }
    
    @Override
    protected OutputStream getOutputStream()
    {
        return HttpConnector.getOutputStream(streamableHttpMessage.getChannel());
    }

    @Override
    public void setThrottlingPolicyStatistics(long remainingRequestInCurrentPeriod, long maximumRequestAllowedPerPeriod, long timeUntilNextPeriodInMillis)
    {
        this.remainingRequestInCurrentPeriod = remainingRequestInCurrentPeriod;
        this.maximumRequestAllowedPerPeriod  = maximumRequestAllowedPerPeriod;
        this.timeUntilNextPeriodInMillis = timeUntilNextPeriodInMillis;
    }

    public void sendFailureResponseToClient(MuleEvent event, int httpStatus, String message) throws IOException, MuleException
    {
        final HttpResponse response = new DefaultHttpResponse(
                streamableHttpMessage.getProtocolVersion(),
                HttpResponseStatus.valueOf(httpStatus));
        Charset charset = Charset.forName(getMessageReceiver().getEndpoint().getEncoding());
        response.setContent(ChannelBuffers.copiedBuffer(message, charset));
        Map<String, String> headers = getThrottlingHeaders();
        for (String name : headers.keySet())
        {
            response.addHeader(name, headers.get(name));
        }
        Object toTransform = response;
        if (event != null)
        {
            MuleMessage muleMessage = event.getMessage();
            muleMessage.setPayload(response);
            toTransform = muleMessage;
        }
        
        HttpResponse transformedResponse = transformResponse(toTransform);
        if (event != null)
        {
            getMessageReceiver().httpConnector.write(event, transformedResponse, streamableHttpMessage.getChannel());
        }
        else
        {
            streamableHttpMessage.getChannel().write(transformedResponse);
        }
    }

    private Map<String,String> getThrottlingHeaders()
    {
        Map<String, String> throttlingHeaders = new HashMap<String, String>();
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_LIMIT_HEADER,this.remainingRequestInCurrentPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_REMAINING_HEADER,this.maximumRequestAllowedPerPeriod);
        addToMapIfNotNull(throttlingHeaders, X_RATE_LIMIT_RESET_HEADER,this.timeUntilNextPeriodInMillis);
        return throttlingHeaders;
    }

    private void addToMapIfNotNull(Map<String,String> map, String key, Long value)
    {
        if (value != null)
        {
            map.put(key, String.valueOf(value));
        }
    }

    @Override
    public void messageProcessingEnded()
    {
        messageProcessedLatch.release();
    }


    public void awaitTermination() throws InterruptedException
    {
        this.messageProcessedLatch.await();
    }
}

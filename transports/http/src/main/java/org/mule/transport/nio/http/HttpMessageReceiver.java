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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.resource.spi.work.Work;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.execution.ExecutionCallback;
import org.mule.api.execution.ExecutionTemplate;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageReceiver;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.transport.ConnectException;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.config.WebSocketEndpointConfiguration;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.transport.nio.tcp.ChannelReceiverResource;
import org.mule.transport.nio.tcp.TcpMessageReceiver;
import org.mule.transport.nio.http.notifications.WebSocketNotification;
import org.mule.util.MapUtils;
import org.mule.util.StringUtils;

/**
 * <code>HttpMessageReceiver</code> is a simple HTTP server that can be used to
 * listen for HTTP requests on a particular port.
 */
public class HttpMessageReceiver extends TcpMessageReceiver
{
    public static final class IllegalResourceCreator<T extends StreamableHttpMessage> implements Callable<T>
    {
        public T call() throws Exception
        {
            throw new IllegalStateException("Impossible to create a new StreamableHttpRequest from a chunk");
        }
    }

    protected static class HttpMessageReceiverUpstreamHandler extends TcpMessageReceiverUpstreamHandler
    {
        public HttpMessageReceiverUpstreamHandler(final HttpMessageReceiver receiver)
        {
            super(receiver);
        }

        @Override
        protected void sendServerGreeting(final ChannelStateEvent e) throws UnsupportedEncodingException
        {
            // not supported in HTTP
        }
    }

    protected static class HttpMessageReceiverRouterWorker implements Work
    {
        private final ChannelReceiverResource crr;
        private final HttpMessageReceiver receiver;

        public HttpMessageReceiverRouterWorker(final ChannelReceiverResource crr,
                                               final HttpMessageReceiver receiver)
        {
            this.crr = crr;
            this.receiver = receiver;
        }

        public void run()
        {
            try
            {
                receiver.routeRequest(crr);
            }
            catch (final Exception e)
            {
                receiver.handleException(e);
            }
        }

        public void release()
        {
            // NOOP
        }
    }

    public static final Set<HttpMethod> SUPPORTED_HTTP_METHODS = Collections.unmodifiableSet(new HashSet<HttpMethod>(
        Arrays.asList(HttpMethod.CONNECT, HttpMethod.DELETE, HttpMethod.GET, HttpMethod.HEAD,
            HttpMethod.OPTIONS, HttpMethod.PATCH, HttpMethod.POST, HttpMethod.PUT, HttpMethod.TRACE)));

    protected static final IllegalResourceCreator<StreamableHttpRequest> ILLEGAL_RESOURCE_CREATOR = new IllegalResourceCreator<StreamableHttpRequest>();

    protected final HttpConnector httpConnector;

    public HttpMessageReceiver(final Connector connector,
                               final FlowConstruct flowConstruct,
                               final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        httpConnector = (HttpConnector) connector;
        
        System.out.println("NIO HttpMessageReceiver");
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void initializeMessageFactory() throws InitialisationException
    {
        try
        {
            final HttpMuleMessageFactory factory = (HttpMuleMessageFactory) super.createMuleMessageFactory();

            final boolean enableCookies = MapUtils.getBooleanValue(endpoint.getProperties(),
                HttpConnector.HTTP_ENABLE_COOKIES_PROPERTY, httpConnector.isEnableCookies());
            factory.setEnableCookies(enableCookies);

            final String cookieSpec = MapUtils.getString(endpoint.getProperties(),
                HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, httpConnector.getCookieSpec());
            factory.setCookieSpec(cookieSpec);

            muleMessageFactory = factory;
        }
        catch (final CreateException ce)
        {
            final Message message = MessageFactory.createStaticMessage(ce.getMessage());
            throw new InitialisationException(message, ce, this);
        }
    }

    @Override
    public void doConnect() throws ConnectException
    {
        // If we already have an endpoint listening on this socket don't try and
        // start another server
        if (this.shouldConnect())
        {
            super.doConnect();
        }
    }

    public static MessageReceiver findReceiverByStem(final Map<Object, MessageReceiver> receivers,
                                                     final String uriStr)
    {
        int match = 0;
        MessageReceiver receiver = null;
        for (final Map.Entry<Object, MessageReceiver> e : receivers.entrySet())
        {
            final String key = (String) e.getKey();
            final MessageReceiver candidate = e.getValue();
            if (uriStr.startsWith(key) && match < key.length())
            {
                match = key.length();
                receiver = candidate;
            }
        }
        return receiver;
    }

    protected boolean shouldConnect()
    {
        final StringBuilder requestUri = new StringBuilder(80);
        requestUri.append(endpoint.getProtocol()).append("://");
        requestUri.append(endpoint.getEndpointURI().getHost());
        requestUri.append(':').append(endpoint.getEndpointURI().getPort());
        requestUri.append('*');

        final MessageReceiver[] receivers = connector.getReceivers(requestUri.toString());
        for (final MessageReceiver receiver : receivers)
        {
            if (receiver.isConnected())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    protected ChannelPipelineFactory getPipelineFactory()
    {
        return new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline() throws Exception
            {
                final ChannelPipeline p = Channels.pipeline();
                p.addLast("netty-http-request-decoder", new HttpRequestDecoder());
                p.addLast("netty-http-response-encoder", new HttpResponseEncoder());
                p.addLast("mule-http-message-receiver-handler", new HttpMessageReceiverUpstreamHandler(
                    HttpMessageReceiver.this));
                return p;
            }
        };
    }

    @Override
    protected void handleChannelData(final Channel channel, final Object message) throws Exception
    {
        if (message instanceof HttpRequest)
        {
            handleHttpRequest(channel, (HttpRequest) message);
        }
        else if (message instanceof HttpChunk)
        {
            handleHttpChunk(channel, (HttpChunk) message);
        }
        else if (message instanceof WebSocketFrame)
        {
            handleWebSocketFrame(channel, (WebSocketFrame) message);
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                "Can't handle channel data: %s received on channel: %s", message, channel));
        }
    }

    protected void handleHttpRequest(final Channel channel, final HttpRequest httpRequest) throws Exception
    {
        if (isWebSocketUpgradeRequest(httpRequest))
        {
            handleWebSocketUpgrade(channel, httpRequest);
            return;
        }

        if (!SUPPORTED_HTTP_METHODS.contains(httpRequest.getMethod()))
        {
            final HttpResponse response = new DefaultHttpResponse(httpRequest.getProtocolVersion(),
                HttpResponseStatus.METHOD_NOT_ALLOWED);
            channel.write(response);
            return;
        }

        if (HttpHeaders.is100ContinueExpected(httpRequest))
        {
            send100Continue(channel);
        }

        final StreamableHttpRequest streamableHttpRequest = getChannelReceiverResource(channel,
            new Callable<StreamableHttpRequest>()
            {
                public StreamableHttpRequest call() throws Exception
                {
                    return new StreamableHttpRequest(httpRequest, channel);
                }
            });

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Offered: %s received from channel: %s to: %s", httpRequest, channel,
                streamableHttpRequest));
        }
    }

    protected void handleHttpChunk(final Channel channel, final HttpChunk chunk) throws Exception
    {
        final ChannelBuffer content = chunk.getContent();

        final StreamableHttpRequest streamableHttpRequest = getChannelReceiverResource(channel,
            ILLEGAL_RESOURCE_CREATOR);

        streamableHttpRequest.getChannelInputStream().offer(
            content.readBytes(content.readableBytes()).array());

        if (chunk instanceof HttpChunkTrailer)
        {
            HttpConnector.handleChunkTrailer(streamableHttpRequest, (HttpChunkTrailer) chunk);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Offered: %s received from channel: %s to: %s", chunk, channel,
                streamableHttpRequest));
        }
    }

    protected void handleWebSocketFrame(final Channel channel, final WebSocketFrame webSocketFrame)
        throws Exception
    {
        if (webSocketFrame instanceof PingWebSocketFrame)
        {
            channel.write(new PongWebSocketFrame((webSocketFrame).getBinaryData()));
            return;
        }

        final WebSocketContext webSocketContext = httpConnector.getWebSocketContext(channel);

        if (webSocketContext == null)
        {
            logger.warn(HttpMessages.webSocketContextNotFound(channel, webSocketFrame).toString());
            return;
        }

        if (webSocketFrame instanceof CloseWebSocketFrame)
        {
            webSocketContext.getServerHandshaker().close(channel, (CloseWebSocketFrame) webSocketFrame);
            return;
        }

        if (!webSocketFrame.isFinalFragment())
        {
            // we expect more data
            return;
        }

        final WebSocketServerMessage streamableWebSocketRequest = getChannelReceiverResource(channel,
            new Callable<WebSocketServerMessage>()
            {
                public WebSocketServerMessage call() throws Exception
                {
                    return new WebSocketServerMessage(webSocketFrame, webSocketContext);
                }
            });

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Offered: %s received from channel: %s to: %s", webSocketFrame,
                channel, streamableWebSocketRequest));
        }
    }

    protected boolean isWebSocketUpgradeRequest(final HttpRequest httpRequest)
    {
        return HttpConstants.HEADER_UPGRADE.equalsIgnoreCase(httpRequest.getHeader(HttpConstants.HEADER_CONNECTION))
               && HttpConstants.WEBSOCKET_UPGRADE.equalsIgnoreCase(httpRequest.getHeader(HttpConstants.HEADER_UPGRADE));
    }

    protected void handleWebSocketUpgrade(final Channel channel, final HttpRequest httpRequest)
        throws MuleException
    {
        // final String httpRequestUri = httpRequest.getUri();
        // final String receiverUrl = endpoint.getAddress() + httpRequestUri;
        // final URI uri = endpoint.getEndpointURI().getUri().resolve();

        final String path = StringUtils.substringBefore(httpRequest.getUri(), "?");
        final StringBuilder requestUriBuilder = new StringBuilder(80);
        if (path.indexOf("://") == -1)
        {
            requestUriBuilder.append(endpoint.getProtocol()).append("://");
            requestUriBuilder.append(endpoint.getEndpointURI().getHost());
            requestUriBuilder.append(':').append(endpoint.getEndpointURI().getPort());

            if (!"/".equals(path))
            {
                requestUriBuilder.append(path);
            }
        }

        final String requestUri = requestUriBuilder.toString();
        // first check that there is a receiver on the root address
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking up NIO websocket receiver on connector: " + connector.getName()
                         + " with URI key: " + requestUri);
        }

        final MessageReceiver messageReceiver = findReceiverByStem(requestUri);

        if (messageReceiver == null)
        {
            final HttpResponse response = new DefaultHttpResponse(httpRequest.getProtocolVersion(),
                HttpResponseStatus.NOT_FOUND);
            channel.write(response);
            return;
        }

        final InboundEndpoint messageReceiverEndpoint = messageReceiver.getEndpoint();
        final WebSocketEndpointConfiguration webSocketConfiguration = (WebSocketEndpointConfiguration) messageReceiverEndpoint.getProperty(HttpConnector.PROPERTY_WEBSOCKET_CONFIG);
        if (webSocketConfiguration == null)
        {
            throw new EndpointException(
                HttpMessages.endpointNotConfiguredForWebSockets(messageReceiverEndpoint));
        }

        final WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
            HttpConnector.getWebSocketAddress(messageReceiverEndpoint),
            webSocketConfiguration.getSubprotocols(), webSocketConfiguration.isAllowExtensions(),
            webSocketConfiguration.getMaxFramePayloadLength());

        final WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);
        if (handshaker == null)
        {
            wsFactory.sendUnsupportedWebSocketVersionResponse(channel);
            return;
        }

        final ChannelFuture handshakeFuture = handshaker.handshake(channel, httpRequest);
        handshakeFuture.addListener(WebSocketServerHandshaker.HANDSHAKE_LISTENER);
        handshakeFuture.addListener(new ChannelFutureListener()
        {
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                if (future.isSuccess())
                {
                    final WebSocketContext webSocketContext = new WebSocketContext(webSocketConfiguration,
                        channel, handshaker);

                    httpConnector.fireNotification(new WebSocketNotification(channel.getId(), WebSocketNotification.UPGRADE));
                    httpConnector.registerWebSocketContext(webSocketContext);
                }
            }
        });
    }

    protected void send100Continue(final Channel channel)
    {
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.CONTINUE);
        channel.write(response);
    }

    @Override
    protected Work newRouterWorker(final ChannelReceiverResource crr)
    {
        return new HttpMessageReceiverRouterWorker(crr, this);
    }
    
    @Override
    protected Work newDispatcherWorker(ChannelReceiverResource crr)
    {
        if (crr instanceof StreamableHttpRequest)
        {
            return new HttpRequestDispatcherWork(this, getWorkManager(), (StreamableHttpRequest)crr);
        }
        else if (crr instanceof WebSocketServerMessage)
        {
            return new WebSocketRequestDispatcherWork(this, getWorkManager(), (WebSocketServerMessage)crr);
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported ChannelReceiverResource type: " + crr);
        }
    }

    @Override
    protected void applyResponseTransformers(final MuleEvent event) throws MuleException
    {
        // disable HTTP response transformation for WS endpoints
        if (HttpConnector.isWebSocketEndpoint(endpoint))
        {
            return;
        }

        super.applyResponseTransformers(event);
    }

    protected void routeRequest(final ChannelReceiverResource crr) throws MuleException
    {
        final MuleMessage message = createMuleMessage(crr, getEndpoint().getEncoding());
        final MessageReceiver receiver = getTargetReceiver(message, endpoint);

        if (logger.isDebugEnabled())
        {
            logger.debug("Received message: " + message);
        }

        if (crr instanceof StreamableHttpRequest)
        {
            routeHttpRequest((StreamableHttpRequest) crr, message, receiver);
        }
        else if (crr instanceof WebSocketServerMessage)
        {
            routeWebSocketRequest((WebSocketServerMessage) crr, message, receiver);
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported ChannelReceiverResource type: " + crr);
        }
    }

    protected void routeWebSocketRequest(final WebSocketServerMessage webSocketMessage,
                                         final MuleMessage message,
                                         final MessageReceiver receiver) throws MuleException
    {
        if (receiver == null)
        {
            final String reason = HttpMessages.cannotBindToAddress(endpoint.getAddress()).toString();
            final CloseWebSocketFrame closeWebSocketFrame = new CloseWebSocketFrame(
                HttpConstants.WS_POLICY_VIOLATION, reason);
            webSocketMessage.getChannel().write(closeWebSocketFrame);
            return;
        }

        MuleEvent returnEvent = null;
        try
        {
            final ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();
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
            throw new MessagingException(CoreMessages.eventProcessingFailedFor(getReceiverKey()),
                returnEvent, e);
        }
    }

    protected void routeHttpRequest(final StreamableHttpRequest streamableHttpRequest,
                                    final MuleMessage message,
                                    final MessageReceiver receiver) throws MuleException
    {
        HttpResponse response;
        MuleEvent returnEvent = null;

        final String path = (String) message.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);

        // the response only needs to be transformed explicitly if
        // A) the request was not served or B) a null result was returned
        if (receiver != null)
        {
            final String contextPath = HttpConnector.normalizeUrl(receiver.getEndpointURI().getPath());

            message.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, contextPath, PropertyScope.INBOUND);

            message.setProperty(HttpConnector.HTTP_CONTEXT_URI_PROPERTY, receiver.getEndpointURI()
                .getAddress(), PropertyScope.INBOUND);

            message.setProperty(HttpConnector.HTTP_RELATIVE_PATH_PROPERTY,
                processRelativePath(contextPath, path), PropertyScope.INBOUND);

            final ExecutionTemplate<MuleEvent> executionTemplate = createExecutionTemplate();

            try
            {
                returnEvent = executionTemplate.execute(new ExecutionCallback<MuleEvent>()
                {
                    public MuleEvent process() throws Exception
                    {
                        return receiver.routeMessage(message);
                    }
                });

                final MuleMessage returnMessage = returnEvent == null ? null : returnEvent.getMessage();

                final Object tempResponse = (returnMessage != null)
                                                                   ? returnMessage.getPayload()
                                                                   : NullPayload.getInstance();

                // This removes the need for users to explicitly adding the response
                // transformer ObjectToHttpResponse in their config
                if (tempResponse instanceof HttpResponse)
                {
                    response = (HttpResponse) tempResponse;
                }
                else
                {
                    response = transformResponse(returnEvent, tempResponse);
                }
            }
            catch (final Exception e)
            {
                MuleEvent responseEvent = null;
                if (e instanceof MessagingException)
                {
                    responseEvent = ((MessagingException) e).getEvent();
                }
                else
                {
                    getConnector().getMuleContext().getExceptionListener().handleException(e);
                }

                Exception actualException = e;
                if (responseEvent != null
                    && responseEvent.getMessage().getExceptionPayload() != null
                    && responseEvent.getMessage().getExceptionPayload().getException() instanceof MessagingException)
                {
                    actualException = (Exception) responseEvent.getMessage()
                        .getExceptionPayload()
                        .getException();
                }

                int httpStatus = 500;
                if (responseEvent != null)
                {
                    // Response code mappings are loaded from
                    // META-INF/services/org/mule/config/niohttp-exception-mappings.properties
                    final String temp = ExceptionHelper.getErrorMapping(connector.getProtocol(),
                        actualException.getClass(), responseEvent.getMuleContext().getExecutionClassLoader());
                    httpStatus = Integer.valueOf(temp);
                }

                if (actualException instanceof MessagingException)
                {
                    final MuleEvent event = ((MessagingException) actualException).getEvent();
                    response = buildFailureResponse(event, actualException.getMessage(), httpStatus);
                }
                else
                {
                    response = buildFailureResponse(returnEvent, streamableHttpRequest.getProtocolVersion(),
                        HttpResponseStatus.valueOf(httpStatus), actualException.getMessage());
                }
            }
        }
        else
        {
            final EndpointURI uri = endpoint.getEndpointURI();
            final String failedPath = String.format("%s://%s:%d%s", uri.getScheme(), uri.getHost(),
                uri.getPort(), path);
            response = buildFailureResponse(returnEvent, streamableHttpRequest.getProtocolVersion(),
                HttpResponseStatus.NOT_FOUND, HttpMessages.cannotBindToAddress(failedPath).toString());
        }

        try
        {
            final Channel channel = streamableHttpRequest.getChannel();
            final ChannelFuture channelFuture = httpConnector.write(returnEvent, response, channel);

            final boolean shouldCloseChannel = !HttpHeaders.isKeepAlive(response);
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
                        logger.debug(String.format("Wrote %s on channel HTTP %s (should close? %s)",
                            finalResponse, future.getChannel(), shouldCloseChannel));
                    }
                });
            }
        }
        catch (final IOException ioe)
        {
            throw new MessagingException(HttpMessages.failedToWriteChunkedPayload(), returnEvent, ioe);
        }
    }

    /**
     * Check if endpoint has a keep-alive property configured. Note the translation
     * from keep-alive in the schema to keepAlive here.
     */
    protected boolean getEndpointKeepAliveValue(final ImmutableEndpoint ep)
    {
        final String value = (String) ep.getProperty("keepAlive");
        if (value != null)
        {
            return Boolean.parseBoolean(value);
        }
        return true;
    }

    protected HttpResponse buildFailureResponse(final MuleEvent event,
                                                final String description,
                                                final int httpStatusCode) throws MuleException
    {
        event.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,
            Integer.toString(httpStatusCode));
        event.getMessage().setPayload(description);
        return transformResponse(event, event.getMessage());
    }

    protected HttpResponse buildFailureResponse(final MuleEvent event,
                                                final HttpVersion version,
                                                final HttpResponseStatus status,
                                                final String description) throws MuleException
    {
        final HttpResponse response = new StreamableHttpResponse(version, status);
        response.setContent(ChannelBuffers.copiedBuffer(description, Charset.forName(endpoint.getEncoding())));
        return transformResponse(event, response);
    }

    protected String processRelativePath(final String contextPath, final String path)
    {
        final String relativePath = StringUtils.substring(path, StringUtils.length(contextPath));
        return StringUtils.removeStart(relativePath, "/");
    }

    protected MessageReceiver getTargetReceiver(final MuleMessage message, final ImmutableEndpoint ep)
        throws ConnectException
    {
        final String path = StringUtils.substringBefore(
            (String) message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY), "?");

        final StringBuilder requestUriBuilder = new StringBuilder(80);
        if (path.indexOf("://") == -1)
        {
            requestUriBuilder.append(ep.getProtocol()).append("://");
            requestUriBuilder.append(ep.getEndpointURI().getHost());
            requestUriBuilder.append(':').append(ep.getEndpointURI().getPort());

            if (!"/".equals(path))
            {
                requestUriBuilder.append(path);
            }
        }

        final String requestUri = requestUriBuilder.toString();
        // first check that there is a receiver on the root address
        if (logger.isTraceEnabled())
        {
            logger.trace("Looking up receiver on connector: " + connector.getName() + " with URI key: "
                         + requestUri);
        }

        MessageReceiver receiver = connector.lookupReceiver(requestUri);

        // If no receiver on the root and there is a request path, look up the
        // received based on the root plus request path
        if (receiver == null && !"/".equals(path))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Secondary lookup of receiver on connector: " + connector.getName()
                             + " with URI key: " + requestUri);
            }

            receiver = findReceiverByStem(requestUri);
        }

        return receiver;
    }

    protected MessageReceiver findReceiverByStem(final String requestUri)
    {
        final MessageReceiver receiver = findReceiverByStem(connector.getReceivers(), requestUri);

        if ((receiver == null) && (logger.isWarnEnabled()))
        {
            logger.warn("No receiver found with secondary lookup on connector: " + connector.getName()
                        + " with URI key: " + requestUri);
            logger.warn("Receivers on connector are: " + MapUtils.toString(connector.getReceivers(), true));
        }

        return receiver;
    }

    public List<Transformer> getResponseTransportTransformers()
    {
        return this.defaultResponseTransformers;
    }

    protected HttpResponse transformResponse(final MuleEvent event, final Object response)
        throws MuleException
    {
        final MuleMessage message = (response instanceof MuleMessage)
                                                                     ? (MuleMessage) response
                                                                     : new DefaultMuleMessage(response,
                                                                         connector.getMuleContext());

        message.applyTransformers(event, defaultResponseTransformers, StreamableHttpResponse.class);
        return (HttpResponse) message.getPayload();
    }

    void processMessage(AbstractTransportMessageProcessTemplate<HttpMessageReceiver, HttpConnector> messageContext) throws MuleException
    {
        super.processMessage(messageContext,messageContext);
    }
}

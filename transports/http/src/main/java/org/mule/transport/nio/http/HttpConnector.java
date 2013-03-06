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

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.SucceededChannelFuture;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connectable;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.notifications.WebSocketNotification;
import org.mule.transport.nio.tcp.TcpClientFactory;
import org.mule.transport.nio.tcp.TcpClientKey;
import org.mule.transport.nio.tcp.TcpConnector;
import org.mule.util.Base64;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

/**
 * <code>HttpConnector</code> provides a way of receiving and sending http requests
 * and responses. The Connector itself handles dispatching http requests. The
 * <code>HttpMessageReceiver</code> handles the receiving requests and processing of
 * headers This endpoint recognises the following properties -
 * <p/>
 * <ul>
 * <li>hostname - The hostname to send and receive http requests</li>
 * <li>port - The port to listen on. The industry standard is 80 and if this propert
 * is not set it will default to 80</li>
 * <li>proxyHostname - If you access the web through a proxy, this holds the server
 * address</li>
 * <li>proxyPort - The port the proxy is configured on</li>
 * <li>proxyUsername - If the proxy requires authentication supply a username</li>
 * <li>proxyPassword - If the proxy requires authentication supply a password</li>
 * </ul>
 */
public class HttpConnector extends TcpConnector
{
    private static final Log LOGGER = LogFactory.getLog(HttpConnector.class);

    public static final String HTTP = "http";
    public static final String PROTOCOL = "http";
    public static final String HTTP_PREFIX = "http.";

    public static final String WEBSOCKET = "ws";

    /**
     * MuleEvent property to pass back the status for the response
     */
    public static final String HTTP_STATUS_PROPERTY = HTTP_PREFIX + "status";
    public static final String HTTP_REASON_PHRASE_PROPERTY = HTTP_PREFIX + "reason.phrase";
    public static final String HTTP_VERSION_PROPERTY = HTTP_PREFIX + "version";
    public static final String WEBSOCKET_VERSION_PROPERTY = HTTP_PREFIX + "websocket.version";

    /**
     * Encapsulates all the HTTP headers
     */
    public static final String HTTP_HEADERS = HTTP_PREFIX + "headers";

    /**
     * Stores the HTTP query parameters received, supports multiple values per key
     * and both query parameter key and value are unescaped
     */
    public static final String HTTP_QUERY_PARAMS = HTTP_PREFIX + "query.params";

    public static final String HTTP_QUERY_STRING = HTTP_PREFIX + "query.string";

    public static final String HTTP_METHOD_PROPERTY = HTTP_PREFIX + "method";

    /**
     * The path and query portions of the URL being accessed.
     */
    public static final String HTTP_REQUEST_PROPERTY = HTTP_PREFIX + "request";

    /**
     * The path portion of the URL being accessed. No query string is included.
     */
    public static final String HTTP_REQUEST_PATH_PROPERTY = HTTP_PREFIX + "request.path";

    /**
     * The context path of the endpoint being accessed. This is the path that the
     * HTTP endpoint is listening on.
     */
    public static final String HTTP_CONTEXT_PATH_PROPERTY = HTTP_PREFIX + "context.path";

    /**
     * The context URI of the endpoint being accessed. This is the address that the
     * HTTP endpoint is listening on. It includes:
     * [scheme]://[host]:[port][http.context.path]
     */
    public static final String HTTP_CONTEXT_URI_PROPERTY = HTTP_PREFIX + "context.uri";

    /**
     * The relative path of the URI being accessed in relation to the context path
     */
    public static final String HTTP_RELATIVE_PATH_PROPERTY = HTTP_PREFIX + "relative.path";

    public static final String HTTP_SERVLET_REQUEST_PROPERTY = HTTP_PREFIX + "servlet.request";
    public static final String HTTP_SERVLET_RESPONSE_PROPERTY = HTTP_PREFIX + "servlet.response";

    /**
     * Allows the user to set a
     * {@link org.apache.commons.httpclient.params.HttpMethodParams} object in the
     * client request to be set on the HttpMethod request object
     */
    public static final String HTTP_PARAMS_PROPERTY = HTTP_PREFIX + "params";
    public static final String HTTP_GET_BODY_PARAM_PROPERTY = HTTP_PREFIX + "get.body.param";
    public static final String DEFAULT_HTTP_GET_BODY_PARAM_PROPERTY = "body";
    public static final String HTTP_POST_BODY_PARAM_PROPERTY = HTTP_PREFIX + "post.body.param";

    public static final String HTTP_DISABLE_STATUS_CODE_EXCEPTION_CHECK = HTTP_PREFIX
                                                                          + "disable.status.code.exception.check";
    public static final String HTTP_ENCODE_PARAMVALUE = HTTP_PREFIX + "encode.paramvalue";

    public static final Set<String> HTTP_INBOUND_PROPERTIES;

    static
    {
        final Set<String> props = new HashSet<String>();
        props.add(HTTP_CONTEXT_PATH_PROPERTY);
        props.add(HTTP_GET_BODY_PARAM_PROPERTY);
        props.add(HTTP_METHOD_PROPERTY);
        props.add(HTTP_PARAMS_PROPERTY);
        props.add(HTTP_POST_BODY_PARAM_PROPERTY);
        props.add(HTTP_REQUEST_PROPERTY);
        props.add(HTTP_REQUEST_PATH_PROPERTY);
        props.add(HTTP_STATUS_PROPERTY);
        props.add(HTTP_VERSION_PROPERTY);
        props.add(HTTP_ENCODE_PARAMVALUE);
        HTTP_INBOUND_PROPERTIES = props;

        // TODO implement NTLM support
        // AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, NTLMScheme.class);
    }

    public static final String HTTP_COOKIE_SPEC_PROPERTY = "cookieSpec";
    public static final String HTTP_COOKIES_PROPERTY = "cookies";
    public static final String HTTP_ENABLE_COOKIES_PROPERTY = "enableCookies";

    public static final String COOKIE_SPEC_NETSCAPE = "netscape";
    public static final String COOKIE_SPEC_RFC2109 = "rfc2109";
    public static final String COOKIE_SPEC_RFC2965 = "rfc2965";
    public static final List<String> ALL_COOKIE_SPECS = Collections.unmodifiableList(Arrays.asList(
        COOKIE_SPEC_NETSCAPE, COOKIE_SPEC_RFC2109, COOKIE_SPEC_RFC2965));

    public static final String DEFAULT_COOKIE_SPEC = COOKIE_SPEC_RFC2109;

    public static final String PROPERTY_WEBSOCKET_CONFIG = "webSocketConfig";

    // TODO implement proxy support
    private String proxyHostname = null;
    private int proxyPort = HttpConstants.DEFAULT_HTTP_PORT;
    private String proxyUsername = null;
    private String proxyPassword = null;
    private boolean proxyNtlmAuthentication;
    private String cookieSpec = DEFAULT_COOKIE_SPEC;
    private boolean enableCookies = false;
    protected final ConcurrentMap<Channel, WebSocketContext> webSocketContextMap;

    public HttpConnector(final MuleContext context)
    {
        super(context);
        webSocketContextMap = new ConcurrentHashMap<Channel, WebSocketContext>();
        registerSupportedProtocolWithoutPrefix(WEBSOCKET);

        System.out.println("NIO HttpConnector");
    }

    @Override
    protected TcpClientFactory getTcpClientFactory()
    {
        return new HttpClientFactory(this);
    }

    @Override
    protected TcpClientKey newTcpClientKey(final Connectable connectable, final ImmutableEndpoint endpoint)
    {
        if (HttpConnector.isWebSocketEndpoint(endpoint))
        {
            return new WebSocketClientKey(connectable, endpoint);
        }
        else
        {
            return super.newTcpClientKey(connectable, endpoint);
        }
    }

    public ChannelFuture write(final MuleEvent event, final HttpMessage httpMessage, final Channel channel)
        throws IOException
    {
        if (!(httpMessage instanceof StreamableHttpMessage))
        {
            return channel.write(httpMessage);
        }

        final StreamableHttpMessage streamableHttpMessage = (StreamableHttpMessage) httpMessage;

        if (!streamableHttpMessage.hasStreamingContent())
        {
            return channel.write(streamableHttpMessage);
        }

        // write headers first
        channel.write(streamableHttpMessage);

        // then chunked content
        final BufferedOutputStream bos = new BufferedOutputStream(new OutputStream()
        {
            @Override
            public void write(final int b) throws IOException
            {
                final HttpChunk chunk = new DefaultHttpChunk(
                    ChannelBuffers.wrappedBuffer(new byte[]{(byte) b}));
                channel.write(chunk);
            }

            @Override
            public void write(final byte[] b, final int off, final int len) throws IOException
            {
                final HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers.copiedBuffer(b, off, len));
                channel.write(chunk);
            }
        });

        streamableHttpMessage.getStreamingContent().write(event, bos);
        IOUtils.closeQuietly(bos);

        // write the final chunk
        return channel.write(new DefaultHttpChunk(ChannelBuffers.EMPTY_BUFFER));
    }

    public void setupClientAuthorization(final MuleEvent event,
                                         final HttpMessage httpMessage,
                                         final ImmutableEndpoint endpoint,
                                         final InetSocketAddress remoteSocketAddress) throws IOException
    {

        if (event != null && event.getCredentials() != null)
        {
            final MuleMessage msg = event.getMessage();

            // TODO consider adding support for realm (hard because it can't be done
            // as a pre-emptive login but as a result of a failed interaction that
            // resulted in an HTTP 401 from which the server realm can be extracted)
            warnDeprecatedOutboundProperty(msg, HTTP_PREFIX + "auth.scope.realm");

            final String requestHost = remoteSocketAddress.getHostName();
            final int requestPort = remoteSocketAddress.getPort();
            final String requestScheme = endpoint.getEndpointURI().getScheme();

            final String authScopeHost = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.host", requestHost);
            final int authScopePort = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.port", requestPort);
            final String authScopeScheme = msg.getOutboundProperty(HTTP_PREFIX + "auth.scope.scheme",
                requestScheme);

            final boolean shouldAuthenticate = StringUtils.equals(requestHost, authScopeHost)
                                               && requestPort == authScopePort
                                               && StringUtils.equals(requestScheme, authScopeScheme);

            if (shouldAuthenticate)
            {
                final String userInfo = new StringBuilder().append(event.getCredentials().getUsername())
                    .append(':')
                    .append(event.getCredentials().getPassword())
                    .toString();
                final String authHeader = String.format("Basic %s",
                    new String(Base64.encodeBytes(userInfo.getBytes(endpoint.getEncoding()))));
                httpMessage.setHeader(HttpConstants.HEADER_AUTHORIZATION, authHeader);
            }

        }
        else if (endpoint.getEndpointURI().getUserInfo() != null
                 && endpoint.getProperty(HttpConstants.HEADER_AUTHORIZATION) == null)
        {
            final String authHeader = String.format(
                "Basic %s",
                new String(Base64.encodeBytes(endpoint.getEndpointURI()
                    .getUserInfo()
                    .getBytes(endpoint.getEncoding()))));
            httpMessage.setHeader(HttpConstants.HEADER_AUTHORIZATION, authHeader);
        }
    }

    public static int getCookieVersion(final String cookiesSpec)
    {
        final String actualCookieSpec = StringUtils.lowerCase(cookiesSpec);

        if (!ALL_COOKIE_SPECS.contains(StringUtils.lowerCase(actualCookieSpec)))
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("cookieSpec",
                actualCookieSpec).toString());
        }

        return ALL_COOKIE_SPECS.indexOf(actualCookieSpec);
    }

    public static void warnDeprecatedOutboundProperty(final MuleMessage msg, final String headerName)
    {
        final Object value = msg.getOutboundProperty(headerName);
        if (value != null)
        {
            LOGGER.warn("Deprecation warning: ignoring outbound header '"
                        + HttpConnector.HTTP_PARAMS_PROPERTY + "'");
        }
    }

    /**
     * Ensures that the supplied URL starts with a '/'.
     */
    public static String normalizeUrl(final String url)
    {
        if (url == null)
        {
            return "/";
        }

        if (!StringUtils.startsWith(url, "/"))
        {
            return "/" + url;
        }

        return url;
    }

    public static void handleChunkTrailer(final StreamableHttpMessage streamableHttpMessage,
                                          final HttpChunkTrailer chunkTrailer)
    {
        streamableHttpMessage.lastChunkReceived();

        // ignoring trailing headers in last chunk: too late to pass
        // them downstream...
        if (!chunkTrailer.getHeaders().isEmpty())
        {
            LOGGER.warn("Discarding trailing headers: " + chunkTrailer.getHeaders());
        }
    }

    public WebSocketContext getWebSocketContext(final Channel channel)
    {
        return webSocketContextMap.get(channel);
    }

    public void registerWebSocketContext(final WebSocketContext webSocketContext)
    {
        // only websockets with defined group can be registered
        if (webSocketContext.getWebSocketEndpointConfiguration().getGroup() == null)
        {
            return;
        }

        // store the context in the map and register a channel listener
        // to auto-purge it
        final Channel channel = webSocketContext.getChannel();
        webSocketContextMap.put(channel, webSocketContext);
        channel.getCloseFuture().addListener(new ChannelFutureListener()
        {
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                final Channel channel = future.getChannel();
                webSocketContextMap.remove(channel);
                fireNotification(new WebSocketNotification(channel.getId(), WebSocketNotification.UPGRADE_DISCONNECT));
                if (logger.isDebugEnabled())
                {
                    logger.debug("Unregistered after channel closure: " + webSocketContext);
                }
            }
        });

        if (logger.isDebugEnabled())
        {
            logger.debug("Registered: " + webSocketContext);
        }
    }

    /**
     * @return total of channels written to.
     */
    public int writeToWebSocket(final MuleEvent event, final String group) throws Exception
    {
        return writeToWebSocket(event, group, null);
    }

    /**
     * @return total of channels written to.
     */
    public int writeToWebSocket(final MuleEvent event, final String group, final Integer channelId)
        throws Exception
    {
        final MuleMessage message = event == null ? null : event.getMessage();
        return writeToWebSocket(message, group, channelId);
    }

    /**
     * @return total of channels written to.
     */
    public int writeToWebSocket(final MuleMessage message, final String group) throws Exception
    {
        return writeToWebSocket(message, group, null);
    }

    /**
     * @return total of channels written to.
     */
    public int writeToWebSocket(final MuleMessage message, final String group, final Integer channelId)
        throws Exception
    {
        int totalChannelWritten = 0;

        for (final Entry<Channel, WebSocketContext> wsc : webSocketContextMap.entrySet())
        {
            if (wsc.getValue().getWebSocketEndpointConfiguration().getGroup().equals(group))
            {
                final Channel channel = wsc.getKey();
                if ((channelId == null) || (channel.getId().equals(channelId)))
                {
                    writeToWebSocket(message, channel);
                    totalChannelWritten++;
                }
            }
        }

        return totalChannelWritten;
    }

    public ChannelFuture writeToWebSocket(final MuleEvent event, final Channel channel) throws Exception
    {
        final MuleMessage message = event == null ? null : event.getMessage();
        return writeToWebSocket(message, channel);
    }

    public ChannelFuture writeToWebSocket(final MuleMessage message, final Channel channel) throws Exception
    {
        final Object payload = message != null ? message.getPayload() : NullPayload.getInstance();

        WebSocketFrame frame = null;
        if (payload instanceof NullPayload)
        {
            return new SucceededChannelFuture(channel);
        }
        else if (payload instanceof WebSocketFrame)
        {
            frame = (WebSocketFrame) payload;
        }
        else if (payload instanceof CharSequence)
        {
            frame = new TextWebSocketFrame(((CharSequence) payload).toString());
        }
        else if (payload instanceof ChannelBuffer)
        {
            frame = new TextWebSocketFrame((ChannelBuffer) payload);
        }
        else
        {
            frame = new BinaryWebSocketFrame(ChannelBuffers.wrappedBuffer(message.getPayloadAsBytes()));
        }

        final ChannelFuture channelFuture = channel.write(frame);

        if (logger.isDebugEnabled())
        {
            final WebSocketFrame finalFrame = frame;
            logger.debug(String.format("Scheduled writing of %s on WebSocket channel %s", frame, channel));

            channelFuture.addListener(new ChannelFutureListener()
            {
                public void operationComplete(final ChannelFuture future) throws Exception
                {
                    logger.debug(String.format("Wrote %s on WebSocket channel %s", finalFrame,
                        future.getChannel()));
                }
            });
        }

        return channelFuture;
    }

    public static boolean isWebSocketEndpoint(final ImmutableEndpoint endpoint)
    {
        return StringUtils.equalsIgnoreCase(endpoint.getEndpointURI().getScheme(), WEBSOCKET)
               || endpoint.getProperty(HttpConnector.PROPERTY_WEBSOCKET_CONFIG) != null;
    }

    public static String getWebSocketAddress(final ImmutableEndpoint endpoint)
    {
        // this will turn http in ws and https in wss
        return StringUtils.replaceOnce(endpoint.getAddress(), PROTOCOL, WEBSOCKET);
    }

    /**
     * @see org.mule.api.transport.Connector#getProtocol()
     */
    @Override
    public String getProtocol()
    {
        return PROTOCOL;
    }

    public String getProxyHostname()
    {
        return proxyHostname;
    }

    public String getProxyPassword()
    {
        return proxyPassword;
    }

    public int getProxyPort()
    {
        return proxyPort;
    }

    public String getProxyUsername()
    {
        return proxyUsername;
    }

    public void setProxyHostname(final String host)
    {
        proxyHostname = host;
    }

    public void setProxyPassword(final String string)
    {
        proxyPassword = string;
    }

    public void setProxyPort(final int port)
    {
        proxyPort = port;
    }

    public void setProxyUsername(final String string)
    {
        proxyUsername = string;
    }

    public String getCookieSpec()
    {
        return cookieSpec;
    }

    public void setCookieSpec(final String cookieSpec)
    {
        if (!ALL_COOKIE_SPECS.contains(StringUtils.lowerCase(cookieSpec)))
        {
            throw new IllegalArgumentException(CoreMessages.propertyHasInvalidValue("cookieSpec", cookieSpec)
                .toString());
        }
        this.cookieSpec = cookieSpec;
    }

    public boolean isEnableCookies()
    {
        return enableCookies;
    }

    public void setEnableCookies(final boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }

    public boolean isProxyNtlmAuthentication()
    {
        return proxyNtlmAuthentication;
    }

    public void setProxyNtlmAuthentication(final boolean proxyNtlmAuthentication)
    {
        this.proxyNtlmAuthentication = proxyNtlmAuthentication;
    }
}

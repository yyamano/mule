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
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.commons.lang.BooleanUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunkTrailer;
import org.jboss.netty.handler.codec.http.HttpClientCodec;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.ExceptionPayload;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.DispatchException;
import org.mule.config.MuleManifest;
import org.mule.config.i18n.CoreMessages;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.message.DefaultExceptionPayload;
import org.mule.transformer.TransformerChain;
import org.mule.transport.nio.http.HttpMessageReceiver.IllegalResourceCreator;
import org.mule.transport.nio.http.transformers.ObjectToHttpRequest;
import org.mule.transport.nio.tcp.TcpClient;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

/**
 * A Netty powered HTTP client used by {@link HttpMessageDispatcher},
 * {@link HttpMessageRequester} and {@link PollingHttpMessageReceiver} to send and
 * receive messages over HTTP {@link Channel}s. <b>It is not threadsafe.</b>
 */
public class HttpClient extends TcpClient
{
    /**
     * Range start for HTTP error status codes.
     */
    public static final int ERROR_STATUS_CODE_RANGE_START = 400;

    /**
     * Range start for HTTP redirect status codes.
     */
    public static final int REDIRECT_STATUS_CODE_RANGE_START = 300;

    protected static final IllegalResourceCreator<StreamableHttpResponse> ILLEGAL_RESOURCE_CREATOR = new IllegalResourceCreator<StreamableHttpResponse>();

    protected final HttpConnector httpConnector;
    protected final Transformer sendTransformer;
    protected final InetSocketAddress remoteSocketAddress;

    protected boolean keepOpen;
    protected StreamableHttpRequest httpRequest;

    public HttpClient(final HttpConnector httpConnector,
                      final Connectable connectable,
                      final ImmutableEndpoint endpoint) throws CreateException
    {
        super(httpConnector, connectable, endpoint);

        this.httpConnector = httpConnector;
        remoteSocketAddress = buildRemoteSocketAddress();

        final List<Transformer> ts = httpConnector.getDefaultOutboundTransformers(null);
        if (ts.size() == 1)
        {
            this.sendTransformer = ts.get(0);
        }
        else if (ts.size() == 0)
        {
            this.sendTransformer = new ObjectToHttpRequest();
            this.sendTransformer.setMuleContext(httpConnector.getMuleContext());
            this.sendTransformer.setEndpoint(endpoint);
        }
        else
        {
            this.sendTransformer = new TransformerChain(ts);
        }
    }

    @Override
    protected ChannelPipelineFactory getPipelineFactory()
    {
        return new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline() throws Exception
            {
                final ChannelPipeline p = Channels.pipeline();
                p.addLast("netty-http-client-codec", new HttpClientCodec());
                p.addLast("mule-http-client-handler", new TcpClientUpstreamHandler(HttpClient.this));
                return p;
            }
        };
    }

    protected InetSocketAddress buildRemoteSocketAddress()
    {
        final EndpointURI uri = endpoint.getEndpointURI();
        final int actualPort = uri.getPort() >= 0 ? uri.getPort() : getDefaultPort(uri.getScheme());
        return new InetSocketAddress(uri.getHost(), actualPort);
    }

    @Override
    protected InetSocketAddress getRemoteSocketAddress()
    {
        return remoteSocketAddress;
    }

    protected int getDefaultPort(final String scheme)
    {
        if (StringUtils.equalsIgnoreCase(scheme, "HTTP"))
        {
            return 80;
        }
        else if (StringUtils.equalsIgnoreCase(scheme, "HTTPS"))
        {
            return 443;
        }
        else
        {
            return -1;
        }
    }

    @Override
    protected void setUp(final MuleEvent event)
    {
        keepOpen = httpConnector.isKeepSendSocketOpen();
    }

    @Override
    protected void cleanUp()
    {
        httpRequest = null;
    }

    @Override
    public boolean isKeepOpen()
    {
        return keepOpen;
    }

    @Override
    protected MuleMessage retrieveRemoteResponse(final long timeout) throws Exception
    {
        final String requestPath = "/" + StringUtils.defaultString(endpoint.getEndpointURI().getPath());

        final StreamableHttpRequest httpRequest = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.GET, requestPath);

        final DefaultMuleEvent event = new DefaultMuleEvent(new DefaultMuleMessage(httpRequest,
            httpConnector.getMuleContext()), (InboundEndpoint) endpoint, (FlowConstruct) null);

        dispatchAndWaitUntilDispatched(event);
        return retrieveRemoteResponse(event, timeout);
    }

    @Override
    protected MuleMessage retrieveRemoteResponse(final MuleEvent event) throws Exception
    {
        final MuleMessage response = super.retrieveRemoteResponse(event);

        if (NumberUtils.toInt(response.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY)) >= REDIRECT_STATUS_CODE_RANGE_START)
        {
            try
            {
                return handleRedirect(response, event);
            }
            catch (final Exception e)
            {
                return setExceptionPayload(response, event, e);
            }
        }
        else
        {
            return response;
        }
    }

    @Override
    protected ChannelFuture doDispatch(final MuleEvent event) throws Exception
    {
        httpRequest = getHttpRequest(event);

        if (HttpUtils.hasProtocolOlderThan(httpRequest, HttpVersion.HTTP_1_1))
        {
            keepOpen = false;
        }

        httpConnector.setupClientAuthorization(event, httpRequest, endpoint, remoteSocketAddress);
        processMuleSession(event, httpRequest);
        processCookies(event, httpRequest);

        httpRequest.setHeader(HttpConstants.HEADER_HOST, remoteSocketAddress.getHostName());

        if (!isKeepOpen())
        {
            httpRequest.setHeader(HttpConstants.HEADER_CONNECTION, "close");
        }

        if (!httpRequest.containsHeader(HttpConstants.HEADER_USER_AGENT))
        {
            httpRequest.setHeader(HttpConstants.HEADER_USER_AGENT, getMuleUserAgent());
        }

        return httpConnector.write(event, httpRequest, channel);
    }

    protected void processCookies(final MuleEvent event, final HttpRequest httpRequest)
    {
        final MuleMessage msg = event.getMessage();

        final Object inboundCookies = msg.getInboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        CookieHelper.addCookiesToHttpMessage(httpRequest, inboundCookies,
            HttpConnector.getCookieVersion(msg.getInboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY,
                httpConnector.getCookieSpec())), event);

        final Object outboundCookies = msg.getOutboundProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        CookieHelper.addCookiesToHttpMessage(httpRequest, outboundCookies,
            HttpConnector.getCookieVersion(msg.getOutboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY,
                httpConnector.getCookieSpec())), event);

        final Object endpointCookies = endpoint.getProperty(HttpConnector.HTTP_COOKIES_PROPERTY);
        final String endpointCookieSpec = StringUtils.defaultIfEmpty(
            (String) endpoint.getProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY),
            httpConnector.getCookieSpec());
        CookieHelper.addCookiesToHttpMessage(httpRequest, endpointCookies,
            HttpConnector.getCookieVersion(endpointCookieSpec), event);
    }

    protected String getMuleUserAgent()
    {
        final Manifest mf = MuleManifest.getManifest();
        final Attributes att = mf.getMainAttributes();
        final StringBuilder userAgent = new StringBuilder("Mule ESB / ");
        if (att.values().size() > 0)
        {
            final String notset = CoreMessages.notSet().getMessage();
            userAgent.append(CoreMessages.version().getMessage() + " Build: "
                             + StringUtils.defaultString(MuleManifest.getBuildNumber(), notset));
        }
        else
        {
            userAgent.append(CoreMessages.versionNotSet().getMessage());
        }

        return userAgent.toString();
    }

    protected StreamableHttpRequest getHttpRequest(final MuleEvent event) throws TransformerException
    {
        final Object payload = event.getMessage().getPayload();

        if (payload instanceof StreamableHttpRequest)
        {
            return (StreamableHttpRequest) payload;
        }
        else
        {
            return (StreamableHttpRequest) sendTransformer.transform(payload);
        }
    }

    protected void processMuleSession(final MuleEvent event, final HttpRequest httpRequest)
    {
        final String muleSession = event.getMessage().getOutboundProperty(
            MuleProperties.MULE_SESSION_PROPERTY);

        if (StringUtils.isNotBlank(muleSession))
        {
            httpRequest.setHeader(HttpConstants.HEADER_MULE_SESSION, muleSession);
        }
    }

    @Override
    protected void handleChannelData(final Channel channel, final Object message) throws Exception
    {
        if (message instanceof HttpResponse)
        {
            final HttpResponse httpResponse = (HttpResponse) message;

            getChannelReceiverResource(channel, new Callable<StreamableHttpResponse>()
            {
                public StreamableHttpResponse call() throws Exception
                {
                    if ((HttpUtils.hasProtocolOlderThan(httpResponse, HttpVersion.HTTP_1_1))
                        || (StringUtils.equalsIgnoreCase(
                            httpResponse.getHeader(HttpConstants.HEADER_CONNECTION), "close")))
                    {
                        keepOpen = false;
                    }

                    return new StreamableHttpResponse(httpResponse, httpRequest, channel);
                }
            });
            messageDeliveredLatch.countDown();
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Delivered: %s received from channel: %s", httpRequest, channel));
            }
        }
        else if (message instanceof HttpChunk)
        {
            final HttpChunk chunk = (HttpChunk) message;
            final ChannelBuffer content = chunk.getContent();

            final StreamableHttpResponse streamableHttpResponse = getChannelReceiverResource(channel,
                ILLEGAL_RESOURCE_CREATOR);

            streamableHttpResponse.getChannelInputStream().offer(
                content.readBytes(content.readableBytes()).array());

            if (chunk instanceof HttpChunkTrailer)
            {
                HttpConnector.handleChunkTrailer(streamableHttpResponse, (HttpChunkTrailer) chunk);
            }
        }
        else
        {
            throw new IllegalArgumentException(String.format(
                "Can't handle channel data: %s received on channel: %s", message, channel));
        }
    }

    @Override
    protected Object buildResponseTransportMessage(final Object response)
    {
        return response;
    }

    protected MuleMessage handleRedirect(final MuleMessage result, final MuleEvent event)
        throws MuleException, IOException
    {
        final String followRedirects = (String) endpoint.getProperty("followRedirects");
        if (!BooleanUtils.toBoolean(followRedirects))
        {
            if (logger.isInfoEnabled())
            {
                logger.info("Received a redirect, but followRedirects=false. Response code: "
                            + result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY) + " "
                            + result.getInboundProperty(HttpConnector.HTTP_REASON_PHRASE_PROPERTY));
            }
            return setExceptionPayload(result, event, null);
        }

        final String locationHeader = result.getInboundProperty(HttpConstants.HEADER_LOCATION);
        if (StringUtils.isBlank(locationHeader))
        {
            throw new HttpResponseException(
                (String) result.getInboundProperty(HttpConnector.HTTP_REASON_PHRASE_PROPERTY),
                NumberUtils.toInt(result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY)));
        }

        final OutboundEndpoint out = new EndpointURIEndpointBuilder(locationHeader,
            httpConnector.getMuleContext()).buildOutboundEndpoint();
        final MuleEvent redirectResult = out.process(event);
        return redirectResult != null ? redirectResult.getMessage() : null;
    }

    protected MuleMessage setExceptionPayload(final MuleMessage result,
                                              final MuleEvent event,
                                              final Exception e) throws IOException, MuleException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("HTTP response is: "
                         + result.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
        }

        final MessageProcessor mp = endpoint instanceof MessageProcessor ? (MessageProcessor) endpoint : null;
        final ExceptionPayload ep = new DefaultExceptionPayload(new DispatchException(event, mp, e));
        result.setExceptionPayload(ep);
        return result;
    }
}

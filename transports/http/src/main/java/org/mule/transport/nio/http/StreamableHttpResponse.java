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

import java.util.Collection;
import java.util.Map.Entry;

import org.apache.commons.lang.Validate;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Adds streaming support to Netty's {@link HttpResponse}. Also supports to be
 * associated with a {@link StreamableHttpRequest} in order to deal with responses of
 * HTTP requests sent by Mule. As such it acts very much like Commons Http Client
 * HttpMethod, which encompasses both the request and the response.
 */
public class StreamableHttpResponse extends AbstractStreamableHttpMessage implements HttpResponse
{
    protected final StreamableHttpRequest streamableHttpRequest;
    protected HttpResponseStatus status;

    public StreamableHttpResponse(final HttpVersion version, final HttpResponseStatus status)
    {
        this(version, status, null, null, null, false, null);
    }

    public StreamableHttpResponse(final HttpResponse response)
    {
        this(response, null, null);
    }

    public StreamableHttpResponse(final HttpResponse response,
                                  final StreamableHttpRequest request,
                                  final Channel channel)
    {
        this(response.getProtocolVersion(), response.getStatus(), request, response.getHeaders(),
            response.getContent(), response.isChunked(), channel);
    }

    StreamableHttpResponse(final HttpVersion version,
                           final HttpResponseStatus status,
                           final StreamableHttpRequest streamableHttpRequest,
                           final Collection<Entry<String, String>> headers,
                           final ChannelBuffer content,
                           final boolean chunked,
                           final Channel channel)
    {
        super(version, headers, content, chunked, channel);
        this.streamableHttpRequest = streamableHttpRequest;
        setStatus(status);
        initiliase();
    }

    public StreamableHttpRequest getStreamableHttpRequest()
    {
        return streamableHttpRequest;
    }

    public HttpResponseStatus getStatus()
    {
        return status;
    }

    public void setStatus(final HttpResponseStatus status)
    {
        Validate.notNull(status, "status can't be null");
        this.status = status;
    }
}

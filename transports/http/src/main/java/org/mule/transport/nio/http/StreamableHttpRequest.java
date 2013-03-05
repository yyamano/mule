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
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * Adds streaming support to Netty's {@link HttpRequest}.
 */
public class StreamableHttpRequest extends AbstractStreamableHttpMessage implements HttpRequest
{
    protected HttpMethod method;
    protected String uri;

    public StreamableHttpRequest(final HttpRequest request, final Channel channel)
    {
        this(request.getProtocolVersion(), request.getMethod(), request.getUri(), request.getHeaders(),
            request.getContent(), request.isChunked(), channel);

    }

    public StreamableHttpRequest(final HttpVersion version, final HttpMethod method, final String uri)
    {
        this(version, method, uri, null, null, false, null);
    }

    StreamableHttpRequest(final HttpVersion version,
                          final HttpMethod method,
                          final String uri,
                          final Collection<Entry<String, String>> headers,
                          final ChannelBuffer content,
                          final boolean chunked,
                          final Channel channel)
    {
        super(version, headers, content, chunked, channel);
        setMethod(method);
        setUri(uri);
        initiliase();
    }

    public HttpMethod getMethod()
    {
        return method;
    }

    public void setMethod(final HttpMethod method)
    {
        Validate.notNull(method, "method can't be null");
        this.method = method;
    }

    public String getUri()
    {
        return uri;
    }

    public void setUri(final String uri)
    {
        Validate.notNull(uri, "uri can't be null");
        this.uri = uri;
    }
}

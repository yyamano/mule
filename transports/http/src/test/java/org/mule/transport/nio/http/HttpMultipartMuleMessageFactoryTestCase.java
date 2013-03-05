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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

public class HttpMultipartMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final String MULTIPART_BOUNDARY = "----------------------------299df9f9431b";
    private static final Map<String, String> TEST_HEADERS = Collections.singletonMap("Content-Type",
        "multipart/form-data; boundary=" + MULTIPART_BOUNDARY);
    private static final String MULTIPART_MESSAGE = "--"
                                                    + MULTIPART_BOUNDARY
                                                    + "\r\n"
                                                    + "Content-Disposition: form-data; name=\"payload\"; filename=\"payload\"\r\n"
                                                    + "Content-Type: application/octet-stream\r\n\r\n"
                                                    + "part payload\r\n\r\n"
                                                    + "--"
                                                    + MULTIPART_BOUNDARY
                                                    + "\r\n"
                                                    + "Content-Disposition: form-data; name=\"two\"; filename=\"two\"\r\n"
                                                    + "Content-Type: application/octet-stream\r\n\r\n"
                                                    + "part two\r\n\r\n" + "--" + MULTIPART_BOUNDARY
                                                    + "--\r\n\r\n";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new HttpMultipartMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.POST, "/services/Echo", null, null, false, mock(Channel.class));
        addHeaders(TEST_HEADERS, request);
        return request;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for HttpMuleMessageFactory";
    }

    @Override
    @Test
    public void testValidPayload() throws Exception
    {
        final HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();
        final HttpRequest request = createMultiPartHttpRequest();
        final MuleMessage message = factory.create(request, encoding);

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof InputStream);
        assertNotNull(message.getInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS));
    }

    private void addHeaders(final Map<String, String> headers, final StreamableHttpMessage shm)
    {
        for (final Entry<String, String> header : headers.entrySet())
        {
            shm.addHeader(header.getKey(), header.getValue());
        }
    }

    private HttpRequest createMultiPartHttpRequest() throws Exception
    {
        final Channel channel = mock(Channel.class);
        when(channel.getRemoteAddress()).thenReturn(new InetSocketAddress(12345));

        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.POST, "/services/Echo", null, null, false, channel);

        addHeaders(TEST_HEADERS, request);
        request.setContent(ChannelBuffers.copiedBuffer(MULTIPART_MESSAGE, Charset.defaultCharset()));
        return request;
    }
}

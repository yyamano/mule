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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactoryTestCase;

public class HttpMuleMessageFactoryTestCase extends AbstractMuleMessageFactoryTestCase
{
    private static final Map<String, String> TEST_HEADERS = Collections.singletonMap("foo-header",
        "foo-value");
    private static final String MULTIPART_BOUNDARY = "------------------------------2eab2c5d5c7e";
    private static final String MULTIPART_MESSAGE = MULTIPART_BOUNDARY + "\n"
                                                    + "Content-Disposition: form-data; name=\"payload\"\n"
                                                    + TEST_MESSAGE + "\n" + MULTIPART_BOUNDARY + "--";
    private static final String REQUEST = "/services/Echo?name=John&lastname=Galt";

    @Override
    protected MuleMessageFactory doCreateMuleMessageFactory()
    {
        return new HttpMuleMessageFactory(muleContext);
    }

    @Override
    protected Object getValidTransportMessage() throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
            "/services/Echo", null, null, false, mock(Channel.class));
        addHeaders(TEST_HEADERS, request);
        return request;
    }

    @Override
    protected Object getUnsupportedTransportMessage()
    {
        return "this is not a valid transport message for HttpMuleMessageFactory";
    }

    @Override
    public void testValidPayload() throws Exception
    {
        final MuleMessageFactory factory = createMuleMessageFactory();

        final Object payload = getValidTransportMessage();
        final MuleMessage message = factory.create(payload, encoding);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayload());
        // note that on this level it's only message factory, and it adds messages
        // from http request to the inbound scope
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals("foo-value", message.getInboundProperty("foo-header"));
    }

    @Test(expected = MessageTypeNotSupportedException.class)
    public void testInvalidPayloadOnHttpMuleMessageFactory() throws Exception
    {
        final HttpMuleMessageFactory factory = new HttpMuleMessageFactory(muleContext);
        factory.create(getUnsupportedTransportMessage(), encoding);
    }

    @Test
    public void testHttpRequestPostPayload() throws Exception
    {
        final HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();

        final HttpRequest request = createPostHttpRequest();
        final MuleMessage message = factory.create(request, encoding);
        assertNotNull(message);
        assertEquals(byte[].class, message.getPayload().getClass());
        final byte[] payload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(TEST_MESSAGE.getBytes(), payload));
    }

    @Test
    public void testHttpRequestMultiPartPayload() throws Exception
    {
        final HttpMuleMessageFactory factory = (HttpMuleMessageFactory) createMuleMessageFactory();

        final HttpRequest request = createMultiPartHttpRequest();
        final MuleMessage message = factory.create(request, encoding);
        assertNotNull(message);
        assertEquals(byte[].class, message.getPayload().getClass());
        final byte[] payload = (byte[]) message.getPayload();
        assertTrue(Arrays.equals(MULTIPART_MESSAGE.getBytes(), payload));
    }

    @Test
    public void testHttpMethodGet() throws Exception
    {
        final InputStream body = new ByteArrayInputStream("/services/Echo".getBytes());
        final HttpResponse response = createHttpRequestAndResponse(HttpConstants.METHOD_GET, body);

        final MuleMessageFactory factory = createMuleMessageFactory();
        final MuleMessage message = factory.create(response, encoding);
        assertNotNull(message);
        assertEquals("/services/Echo", message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_GET, message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(),
            message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testHttpMethodPost() throws Exception
    {
        final InputStream body = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        final HttpResponse response = createHttpRequestAndResponse(HttpConstants.METHOD_POST, body);

        final MuleMessageFactory factory = createMuleMessageFactory();
        final MuleMessage message = factory.create(response, encoding);
        assertNotNull(message);
        assertEquals(TEST_MESSAGE, message.getPayloadAsString());
        assertEquals(HttpConstants.METHOD_POST,
            message.getInboundProperty(HttpConnector.HTTP_METHOD_PROPERTY));
        assertEquals(HttpVersion.HTTP_1_1.toString(),
            message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY));
        assertEquals("200", message.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY));
    }

    @Test
    public void testQueryParamProperties() throws Exception
    {
        final HttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, REQUEST,
            TEST_HEADERS.entrySet(), null, false, mock(Channel.class));

        final MuleMessageFactory factory = createMuleMessageFactory();
        final MuleMessage message = factory.create(request, encoding);
        @SuppressWarnings("unchecked")
        final Map<String, Object> queryParams = (Map<String, Object>) message.getInboundProperty(HttpConnector.HTTP_QUERY_PARAMS);
        assertNotNull(queryParams);
        assertEquals("John", queryParams.get("name"));
        assertEquals("John", message.getInboundProperty("name"));
        assertEquals("Galt", queryParams.get("lastname"));
        assertEquals("Galt", message.getInboundProperty("lastname"));

        assertEquals("name=John&lastname=Galt", message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING));
    }

    @Test
    public void testHeaderProperties() throws Exception
    {
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("foo-header", "foo-value");
        headers.put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        headers.put("Host", "localhost");

        final HttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
            "/services/Echo", headers.entrySet(), null, false, mock(Channel.class));

        final MuleMessageFactory factory = createMuleMessageFactory();
        final MuleMessage message = factory.create(request, encoding);
        final Map<String, Object> httpHeaders = message.getInboundProperty(HttpConnector.HTTP_HEADERS);
        assertNotNull(headers);
        assertEquals("foo-value", httpHeaders.get("foo-header"));
        assertEquals("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)",
            httpHeaders.get("User-Agent"));
        assertEquals("localhost", httpHeaders.get("Host"));
        assertEquals("false", httpHeaders.get("Keep-Alive"));
        assertEquals("false", httpHeaders.get("Connection"));
        assertEquals("", message.getInboundProperty(HttpConnector.HTTP_QUERY_STRING));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMultipleHeaderWithSameName() throws Exception
    {
        final HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory(null);

        final List<Entry<String, String>> headers = new ArrayList<Entry<String, String>>();
        headers.add(new DefaultMapEntry("k2", "priority"));
        headers.add(new DefaultMapEntry("k1", "top"));
        headers.add(new DefaultMapEntry("k2", "always"));
        headers.add(new DefaultMapEntry("k2", "true"));

        final Map<String, Object> parsedHeaders = messageFactory.convertHeadersToMap(headers);

        assertEquals(2, parsedHeaders.size());
        assertEquals("top", parsedHeaders.get("k1"));
        assertEquals("priority,always,true", parsedHeaders.get("k2"));
    }

    @Test
    public void testProcessQueryParams() throws UnsupportedEncodingException
    {
        final HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory(null);

        final String queryParams = "key1=value1&key2=value2&key1=value4&key3=value3&key1=value5";
        final Map<String, Object> processedParams = messageFactory.processQueryParams(
            "http://localhost:8080/resources?" + queryParams, "UTF-8");

        final Object value1 = processedParams.get("key1");
        assertNotNull(value1);
        assertTrue(value1 instanceof List);
        assertTrue(((List<?>) value1).contains("value1"));
        assertTrue(((List<?>) value1).contains("value4"));
        assertTrue(((List<?>) value1).contains("value5"));

        final Object value2 = processedParams.get("key2");
        assertNotNull(value2);
        assertEquals("value2", value2);

        final Object value3 = processedParams.get("key3");
        assertNotNull(value3);
        assertEquals("value3", value3);

    }

    @Test
    public void testProcessEscapedQueryParams() throws UnsupportedEncodingException
    {
        final HttpMuleMessageFactory messageFactory = new HttpMuleMessageFactory(null);

        final String queryParams = "key1=value%201&key2=value2&key%203=value3&key%203=value4";
        final Map<String, Object> processedParams = messageFactory.processQueryParams(
            "http://localhost:8080/resources?" + queryParams, "UTF-8");

        final Object value1 = processedParams.get("key1");
        assertNotNull(value1);
        assertEquals("value 1", value1);

        final Object value2 = processedParams.get("key2");
        assertNotNull(value2);
        assertEquals("value2", value2);

        final Object value3 = processedParams.get("key 3");
        assertNotNull(value3);
        assertTrue(value3 instanceof List);
        assertTrue(((List<?>) value3).contains("value3"));
        assertTrue(((List<?>) value3).contains("value4"));

    }

    private void addHeaders(final Map<String, String> headers, final StreamableHttpMessage shm)
    {
        for (final Entry<String, String> header : headers.entrySet())
        {
            shm.addHeader(header.getKey(), header.getValue());
        }
    }

    private HttpRequest createPostHttpRequest() throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.POST, "/services/Echo", null, null, false, mock(Channel.class));
        addHeaders(TEST_HEADERS, request);
        request.setContent(ChannelBuffers.copiedBuffer(TEST_MESSAGE, Charset.defaultCharset()));
        return request;
    }

    private HttpRequest createMultiPartHttpRequest() throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.POST, "/services/Echo", null, null, false, mock(Channel.class));
        final Map<String, String> headers = Collections.singletonMap("Content-Type",
            "multipart/form-data; boundary=" + MULTIPART_BOUNDARY.substring(2));
        addHeaders(headers, request);
        request.setContent(ChannelBuffers.copiedBuffer(MULTIPART_MESSAGE, Charset.defaultCharset()));
        return request;
    }

    private HttpResponse createHttpRequestAndResponse(final String method, final InputStream body)
        throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(method), "/services/Echo", null, null, false, mock(Channel.class));

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK, request, null, null, false, mock(Channel.class))
        {
            @Override
            public Object getPayload()
            {
                return body;
            }
        };

        addHeaders(TEST_HEADERS, response);

        return response;
    }
}

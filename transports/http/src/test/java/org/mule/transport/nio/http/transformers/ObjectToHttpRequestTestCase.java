/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.transformers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Rule;
import org.junit.Test;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.StreamableHttpRequest;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@SuppressWarnings("deprecation")
@NioTest
public class ObjectToHttpRequestTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    private InboundEndpoint endpoint;

    private MuleMessage setupRequestContext(final String url, final String method) throws Exception
    {
        final StreamableHttpRequest request = new StreamableHttpRequest(HttpVersion.HTTP_1_1,
            HttpMethod.valueOf(method), url);

        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(url);

        final MuleEvent event = getTestEvent(request, endpoint);
        final MuleMessage message = event.getMessage();
        message.setOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        message.setOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY, url);
        RequestContext.setEvent(event);

        return message;
    }

    private ObjectToHttpRequest createTransformer() throws Exception
    {
        final ObjectToHttpRequest transformer = new ObjectToHttpRequest();
        transformer.setMuleContext(muleContext);
        transformer.setEndpoint(endpoint);
        transformer.initialise();
        return transformer;
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    @Test
    public void testUrlWithoutQuery() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://localhost:8080/services",
            HttpConstants.METHOD_GET);
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals(null, getQueryString(httpRequest));
    }

    @Test
    public void testUrlWithQuery() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://localhost:8080/services?method=echo",
            HttpConstants.METHOD_GET);
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals("method=echo", getQueryString(httpRequest));
    }

    @Test
    public void testUrlWithUnescapedQuery() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://mycompany.com/test?fruits=apple%20orange",
            HttpConstants.METHOD_GET);
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals("fruits=apple%20orange", getQueryString(httpRequest));
    }

    @Test
    public void testUrlWithFragment() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://mycompany.com/test#cool",
            HttpConstants.METHOD_GET);
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals("/test#cool", httpRequest.getUri());
    }

    @Test
    public void testUrlWithQueryAndFragment() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://mycompany.com/test?fruits=apple#cool",
            HttpConstants.METHOD_GET);
        // transforming NullPayload will make sure that no body=xxx query is added
        message.setPayload(NullPayload.getInstance());

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals("/test?fruits=apple#cool", httpRequest.getUri());
    }

    @Test
    public void testAppendedUrl() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://mycompany.com/test?fruits=apple%20orange",
            HttpConstants.METHOD_GET);
        // transforming a payload here will add it as body=xxx query parameter
        message.setPayload("test");
        message.setOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, "body");

        final ObjectToHttpRequest transformer = createTransformer();
        final Object response = transformer.transform(message);

        assertTrue(response instanceof StreamableHttpRequest);
        final StreamableHttpRequest httpRequest = (StreamableHttpRequest) response;
        assertEquals("fruits=apple%20orange&body=test", getQueryString(httpRequest));
    }

    @Test
    public void testEncodingOfParamValueTriggeredByMessageProperty() throws Exception
    {
        // the payload is already encoded, switch off encoding it in the transformer
        final String encodedPayload = "encoded%20payload";
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://mycompany.com/", "GET");
        message.setOutboundProperty(HttpConnector.HTTP_ENCODE_PARAMVALUE, false);
        message.setOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY, "body");
        message.setPayload(encodedPayload);

        final ObjectToHttpRequest transformer = createTransformer();
        final Object result = transformer.transform(message);

        assertTrue(result instanceof StreamableHttpRequest);
        final StreamableHttpRequest request = (StreamableHttpRequest) result;
        assertEquals(HttpMethod.GET, request.getMethod());

        final String expected = "body=" + encodedPayload;
        assertEquals(expected, getQueryString(request));
    }

    @Test
    public void testPostMethod() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://localhost:8080/services",
            HttpConstants.METHOD_POST);
        final String contentType = "text/plain";

        message.setPayload("I'm a payload");
        message.setInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE, contentType);

        final ObjectToHttpRequest transformer = createTransformer();
        final Object result = transformer.transform(message);

        assertTrue(result instanceof StreamableHttpRequest);
        final StreamableHttpRequest request = (StreamableHttpRequest) result;
        assertEquals(HttpMethod.POST, request.getMethod());

        assertEquals(null, getQueryString(request));
        assertEquals(contentType, request.getHeader(HttpConstants.HEADER_CONTENT_TYPE));
    }

    @Test
    public void testPutMethod() throws Exception
    {
        final MuleMessage message = setupRequestContext(HttpConnector.HTTP + "://localhost:8080/services",
            HttpConstants.METHOD_PUT);
        final String contentType = "text/plain";

        message.setPayload("I'm a payload");
        message.setInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE, contentType);

        final ObjectToHttpRequest transformer = createTransformer();
        final Object result = transformer.transform(message);

        assertTrue(result instanceof StreamableHttpRequest);
        final StreamableHttpRequest request = (StreamableHttpRequest) result;
        assertEquals(HttpMethod.PUT, request.getMethod());

        assertEquals(null, getQueryString(request));
        assertEquals(contentType, request.getHeader(HttpConstants.HEADER_CONTENT_TYPE));
    }

    private String getQueryString(final StreamableHttpRequest httpRequest) throws URISyntaxException
    {
        return new URI(httpRequest.getUri()).getRawQuery();
    }
}

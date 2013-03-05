/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.nio.http.CacheControlHeader;
import org.mule.transport.nio.http.CookieHelper;
import org.mule.transport.nio.http.CookieWrapper;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.StreamableHttpResponse;

@SmallTest
public class HttpResponseBuilderTestCase extends AbstractMuleTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";
    private static final String HEADER_STATUS = "#[header:status]";
    private static final String HEADER_CONTENT_TYPE = "#[header:contentType]";
    private static final String HEADER_CACHE_CONTROL = "#[header:cacheControl]";
    private static final String HEADER_EXPIRES = "#[header:expires]";
    private static final String HEADER_LOCATION = "#[header:location]";
    private static final String HEADER_NAME = "#[header:name]";
    private static final String HEADER_VALUE = "#[header:value]";
    private static final String HEADER_DOMAIN = "#[header:domain]";
    private static final String HEADER_PATH = "#[header:path]";
    private static final String HEADER_EXPIRY_DATE = "#[header:expiryDate]";
    private static final String HEADER_SECURE = "#[header:secure]";
    private static final String HEADER_VERSION = "#[header:version]";
    private static final String HEADER_DIRECTIVE = "#[header:directive]";
    private static final String HEADER_MAX_AGE = "#[header:maxAge]";
    private static final String HEADER_MUST_REVALIDATE = "#[header:mustRevalidate]";
    private static final String HEADER_NO_CACHE = "#[header:noCache]";
    private static final String HEADER_NO_STORE = "#[header:noStore]";

    private MuleContext muleContext;
    private MuleEvent mockMuleEvent;
    private MuleMessage mockMuleMessage;
    private ExpressionManager mockExpressionManager = Mockito.mock(ExpressionManager.class);

    @Before
    public void setUp()
    {
        muleContext = mock(MuleContext.class);
        mockMuleEvent = mock(MuleEvent.class);
        when(mockMuleEvent.getMuleContext()).thenReturn(muleContext);
        mockMuleMessage = mock(MuleMessage.class);
        when(mockMuleEvent.getMessage()).thenReturn(mockMuleMessage);
        mockExpressionManager = mock(ExpressionManager.class);
        when(muleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test
    public void testEmptyHttpResponseBuilder() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);

        when(mockMuleEvent.getMessage()).thenReturn(muleMessage);

        mockParse();
        final StreamableHttpResponse httpResponse = (StreamableHttpResponse) httpResponseBuilder.process(
            mockMuleEvent)
            .getMessage()
            .getPayload();
        assertEquals(HTTP_BODY, new String((byte[]) httpResponse.getPayload()));
        assertEquals(HttpConstants.HTTP11, httpResponse.getProtocolVersion().toString());
        assertEquals(HttpConstants.SC_OK, httpResponse.getStatus().getCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE,
            HttpConstants.DEFAULT_CONTENT_TYPE);
    }

    @Test
    public void testHttpResponseBuilderAttributes() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        when(mockMuleEvent.getMessage()).thenReturn(muleMessage);

        httpResponseBuilder.setContentType("text/html");
        httpResponseBuilder.setStatus(String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR));

        mockParse();
        final StreamableHttpResponse httpResponse = (StreamableHttpResponse) httpResponseBuilder.process(
            mockMuleEvent)
            .getMessage()
            .getPayload();
        assertEquals(HTTP_BODY, new String((byte[]) httpResponse.getPayload()));
        assertEquals(HttpConstants.HTTP11, httpResponse.getProtocolVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatus().getCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseBuilderAttributesWithExpressions() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        when(mockMuleEvent.getMessage()).thenReturn(muleMessage);

        httpResponseBuilder.setStatus(HEADER_STATUS);
        httpResponseBuilder.setContentType(HEADER_CONTENT_TYPE);

        when(mockExpressionManager.isExpression(HEADER_STATUS)).thenReturn(true);
        when(mockExpressionManager.parse(HEADER_STATUS, mockMuleEvent)).thenReturn(
            String.valueOf(HttpConstants.SC_INTERNAL_SERVER_ERROR));
        when(mockExpressionManager.isExpression(HEADER_CONTENT_TYPE)).thenReturn(true);
        when(mockExpressionManager.parse(HEADER_CONTENT_TYPE, mockMuleEvent)).thenReturn("text/html");

        final StreamableHttpResponse httpResponse = (StreamableHttpResponse) httpResponseBuilder.process(
            mockMuleEvent)
            .getMessage()
            .getPayload();
        assertEquals(HTTP_BODY, new String((byte[]) httpResponse.getPayload()));
        assertEquals(HttpConstants.HTTP11, httpResponse.getProtocolVersion().toString());
        assertEquals(HttpConstants.SC_INTERNAL_SERVER_ERROR, httpResponse.getStatus().getCode());
        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseBuilderHeadersWithExpressions() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", HEADER_CACHE_CONTROL);
        headers.put("Expires", HEADER_EXPIRES);
        headers.put("Location", HEADER_LOCATION);
        httpResponseBuilder.setHeaders(headers);

        when(mockExpressionManager.isExpression(HEADER_CACHE_CONTROL)).thenReturn(true);
        when(mockExpressionManager.parse(HEADER_CACHE_CONTROL, mockMuleEvent)).thenReturn("max-age=3600");

        when(mockExpressionManager.isExpression(HEADER_EXPIRES)).thenReturn(true);
        when(mockExpressionManager.evaluate(HEADER_EXPIRES, mockMuleEvent)).thenReturn(
            "Thu, 01 Dec 1994 16:00:00 GMT");

        when(mockExpressionManager.isExpression(HEADER_LOCATION)).thenReturn(true);
        when(mockExpressionManager.parse(HEADER_LOCATION, mockMuleEvent)).thenReturn("http://localhost:8080");

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setHeaders(response, mockMuleEvent);

        validateHeaders(response.getHeaders());
    }

    @Test
    public void testHttpResponseBuilderHeaders() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("Cache-Control", "max-age=3600");
        headers.put("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        headers.put("Location", "http://localhost:8080");
        httpResponseBuilder.setHeaders(headers);

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setHeaders(response, mockMuleEvent);

        validateHeaders(response.getHeaders());
    }

    @Test
    public void testHttpResponseBuilderHeadersWithExpressionInHeaderName() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(HEADER_LOCATION, "http://localhost:9090");
        httpResponseBuilder.setHeaders(headers);

        when(mockExpressionManager.isExpression(HEADER_LOCATION)).thenReturn(true);
        when(mockExpressionManager.parse(HEADER_LOCATION, mockMuleEvent)).thenReturn("Location");

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setHeaders(response, mockMuleEvent);

        validateHeader(response.getHeaders(), "Location", "http://localhost:9090");
    }

    @Test
    public void testHttpResponseBuilderCookies() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();

        cookies.add(createCookieWrapper("userName", "John_Galt", "localhost", "/",
            "Thu, 15 Dec 2013 16:00:00 GMT", "true", "1"));
        cookies.add(createCookieWrapper("userId", "1", "localhost", "/", "Thu, 01 Dec 2013 16:00:00 GMT",
            "true", "1"));

        httpResponseBuilder.setCookies(cookies);

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCookies(response, mockMuleEvent);

        final Map<String, String> responseCookies = getHeaderCookie(response.getHeaders());
        assertNotNull(responseCookies);

        assertTrue(Pattern.matches(
            "userName=John_Galt;Max-Age=\\d+;Path=\"/\";Domain=localhost;Secure;Version=1",
            responseCookies.get("userName")));

        assertTrue(Pattern.matches("userId=1;Max-Age=\\d+;Path=\"/\";Domain=localhost;Secure;Version=1",
            responseCookies.get("userId")));
    }

    @Test
    public void testHttpResponseBuilderCookiesWithExpressions() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        final List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookieWrapper(HEADER_NAME, HEADER_VALUE, HEADER_DOMAIN, HEADER_PATH,
            HEADER_EXPIRY_DATE, HEADER_SECURE, HEADER_VERSION));
        httpResponseBuilder.setCookies(cookies);

        when(mockExpressionManager.parse(HEADER_NAME, mockMuleEvent)).thenReturn("userName");
        when(mockExpressionManager.parse(HEADER_VALUE, mockMuleEvent)).thenReturn("John_Galt");
        when(mockExpressionManager.parse(HEADER_DOMAIN, mockMuleEvent)).thenReturn("localhost");
        when(mockExpressionManager.parse(HEADER_PATH, mockMuleEvent)).thenReturn("/");
        when(mockExpressionManager.parse(HEADER_SECURE, mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_VERSION, mockMuleEvent)).thenReturn("1");
        when(mockExpressionManager.evaluate(HEADER_EXPIRY_DATE, mockMuleEvent)).thenReturn(
            "Sun, 15 Dec 2013 16:00:00 GMT");

        acceptMockExpressions(HEADER_NAME, HEADER_VALUE, HEADER_DOMAIN, HEADER_PATH, HEADER_EXPIRY_DATE,
            HEADER_SECURE, HEADER_VERSION);

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCookies(response, mockMuleEvent);

        final Map<String, String> responseCookies = getHeaderCookie(response.getHeaders());
        assertNotNull(responseCookies);

        assertTrue(Pattern.matches(
            "userName=John_Galt;Max-Age=\\d+;Path=\"/\";Domain=localhost;Secure;Version=1",
            responseCookies.get("userName")));
    }

    @Test
    public void testHttpResponseDefaultVersion() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        when(mockMuleMessage.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY)).thenReturn(
            HttpConstants.HTTP10);

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);

        httpResponseBuilder.setHttpProtocolVersion(response, mockMuleMessage);

        assertEquals(HttpConstants.HTTP10, response.getProtocolVersion().toString());
    }

    @Test
    public void testHttpResponseDefaultContentType() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        when(mockMuleMessage.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE)).thenReturn("text/html");

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        mockParse();
        httpResponseBuilder.setContentType(response, mockMuleEvent);

        validateHeader(response.getHeaders(), HttpConstants.HEADER_CONTENT_TYPE, "text/html");
    }

    @Test
    public void testHttpResponseEmptyCacheControl() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        httpResponseBuilder.setCacheControl(new CacheControlHeader());

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCacheControl(response, mockMuleEvent);
        assertNull(response.getHeader(HttpConstants.HEADER_CACHE_CONTROL));
    }

    @Test
    public void testHttpResponseCacheControl() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("public");
        cacheControl.setMaxAge("3600");
        cacheControl.setMustRevalidate("true");
        cacheControl.setNoCache("true");
        cacheControl.setNoStore("true");
        httpResponseBuilder.setCacheControl(cacheControl);
        mockParse();

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCacheControl(response, mockMuleEvent);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600",
            response.getHeader(HttpConstants.HEADER_CACHE_CONTROL));
    }

    @Test
    public void testHttpResponseCacheControlWithExpressions() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective(HEADER_DIRECTIVE);
        cacheControl.setMaxAge(HEADER_MAX_AGE);
        cacheControl.setMustRevalidate(HEADER_MUST_REVALIDATE);
        cacheControl.setNoCache(HEADER_NO_CACHE);
        cacheControl.setNoStore(HEADER_NO_STORE);
        httpResponseBuilder.setCacheControl(cacheControl);

        when(mockExpressionManager.parse(HEADER_DIRECTIVE, mockMuleEvent)).thenReturn("public");
        when(mockExpressionManager.parse(HEADER_MAX_AGE, mockMuleEvent)).thenReturn("3600");
        when(mockExpressionManager.parse(HEADER_MUST_REVALIDATE, mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_CACHE, mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_STORE, mockMuleEvent)).thenReturn("true");

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCacheControl(response, mockMuleEvent);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600",
            response.getHeader(HttpConstants.HEADER_CACHE_CONTROL));
    }

    @Test
    public void testHttpResponseCacheControlWithExtension() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setMaxAge("3600");
        httpResponseBuilder.setCacheControl(cacheControl);
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_CACHE_CONTROL, "smax-age=3600");
        httpResponseBuilder.setHeaders(headers);
        mockParse();

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setHeaders(response, mockMuleEvent);
        httpResponseBuilder.setCacheControl(response, mockMuleEvent);

        assertEquals("max-age=3600,smax-age=3600", response.getHeader(HttpConstants.HEADER_CACHE_CONTROL));

    }

    @Test
    public void testHttpResponseCopyOutboundProperties() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final Map<String, Object> outboundProperties = new HashMap<String, Object>();
        outboundProperties.put(HttpConstants.HEADER_AGE, "12");
        outboundProperties.put(HttpConstants.HEADER_CACHE_CONTROL, "max-age=3600");
        outboundProperties.put(MuleProperties.MULE_ENCODING_PROPERTY, "UTF-8");
        final Cookie[] cookies = new Cookie[2];
        cookies[0] = new DefaultCookie("clientId", "2");
        cookies[1] = new DefaultCookie("category", "premium");
        outboundProperties.put(HttpConstants.HEADER_COOKIE_SET, cookies);

        final Set<String> propertyNames = outboundProperties.keySet();
        when(mockMuleMessage.getOutboundPropertyNames()).thenReturn(propertyNames);
        for (final String propertyName : propertyNames)
        {
            when(mockMuleMessage.getOutboundProperty(propertyName)).thenReturn(
                outboundProperties.get(propertyName));
        }

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.copyOutboundProperties(response, mockMuleMessage);

        for (final Entry<String, String> header : response.getHeaders())
        {
            if (HttpConstants.HEADER_COOKIE_SET.equals(header.getKey()))
            {
                if (header.getValue().startsWith(cookies[0].getName()))
                {
                    assertEquals(cookies[0].toString(), header.getValue());
                }
                else
                {
                    assertEquals(cookies[1].toString(), header.getValue());
                }

            }
            else if (header.getKey().startsWith(HttpConstants.CUSTOM_HEADER_PREFIX))
            {
                assertEquals(
                    outboundProperties.get(header.getKey().substring(
                        HttpConstants.CUSTOM_HEADER_PREFIX.length())), header.getValue());
            }
            else
            {
                assertEquals(outboundProperties.get(header.getKey()), header.getValue());
            }
        }
    }

    @Test
    public void testHttpResponseWithOutboundProperties() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setMaxAge("3600");
        httpResponseBuilder.setCacheControl(cacheControl);

        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_CACHE_CONTROL, "public");
        headers.put(HttpConstants.HEADER_AGE, "12");
        httpResponseBuilder.setHeaders(headers);

        final Map<String, Object> outboundProperties = new HashMap<String, Object>();
        outboundProperties.put(HttpConstants.HEADER_CACHE_CONTROL, "no-cache");
        outboundProperties.put(HttpConstants.HEADER_AGE, "20");
        outboundProperties.put(HttpConstants.HEADER_LOCATION, "http://localhost:9090");

        mockParse();
        final DefaultMuleMessage message = new DefaultMuleMessage(HTTP_BODY, outboundProperties, muleContext);

        when(mockMuleEvent.getMessage()).thenReturn(message);
        final StreamableHttpResponse httpResponse = (StreamableHttpResponse) httpResponseBuilder.process(
            mockMuleEvent)
            .getMessage()
            .getPayload();
        final List<Entry<String, String>> resultHeaders = httpResponse.getHeaders();
        validateHeader(resultHeaders, HttpConstants.HEADER_CACHE_CONTROL, "max-age=3600,public");
        validateHeader(resultHeaders, HttpConstants.HEADER_AGE, "12");
        validateHeader(resultHeaders, HttpConstants.HEADER_LOCATION, "http://localhost:9090");
    }

    @Test
    public void testHttpResponseWithDateExpression() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put(HttpConstants.HEADER_EXPIRES, "#[now]");
        httpResponseBuilder.setHeaders(headers);

        final Date now = new Date();

        when(mockExpressionManager.isExpression("#[now]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[now]", mockMuleEvent)).thenReturn(now);

        final StreamableHttpResponse httpResponse = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setHeaders(httpResponse, mockMuleEvent);

        final SimpleDateFormat httpDateFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        httpDateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        validateHeader(httpResponse.getHeaders(), HttpConstants.HEADER_EXPIRES, httpDateFormatter.format(now));
    }

    @Test
    public void testHttpResponseCookieWithDateBuilder() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final Date now = new Date();
        final List<CookieWrapper> cookies = new ArrayList<CookieWrapper>();
        cookies.add(createCookieWrapper("test", "test", null, null, "#[now]", null, null));
        httpResponseBuilder.setCookies(cookies);

        when(mockExpressionManager.isExpression("#[now]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[now]", mockMuleEvent)).thenReturn(now);
        when(mockExpressionManager.parse("test", mockMuleEvent)).thenReturn("test");
        when(mockExpressionManager.parse("test", mockMuleEvent)).thenReturn("test");

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.HTTP_1_1,
            HttpResponseStatus.OK);
        httpResponseBuilder.setCookies(response, mockMuleEvent);

        final SimpleDateFormat httpCookieFormatter = new SimpleDateFormat(CookieHelper.EXPIRE_PATTERN,
            Locale.US);
        httpCookieFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));

        final String expectedCookieValue = "test=test;Max-Age=0;Version=1";
        validateHeader(response.getHeaders(), HttpConstants.HEADER_COOKIE_SET, expectedCookieValue);
    }

    @Test
    public void testHttpResponseSetBodyWithHttpResponsePayload() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(ChannelBuffers.wrappedBuffer(HTTP_BODY.getBytes()));

        when(mockMuleMessage.getPayload()).thenReturn(response);

        final HttpResponse httpResponseBuilt = httpResponseBuilder.createHttpResponse(mockMuleMessage,
            mockMuleEvent);
        assertEquals(HTTP_BODY, httpResponseBuilt.getContent().toString(Charset.defaultCharset()));
    }

    @Test
    public void testHttpResponseSetBody() throws Exception
    {
        final HttpResponseBuilder httpResponseBuilder = createHttpResponseBuilder();

        when(mockMuleMessage.getPayload()).thenReturn(HTTP_BODY);

        final HttpResponse httpResponseBuilt = httpResponseBuilder.createHttpResponse(mockMuleMessage,
            mockMuleEvent);
        assertEquals(HTTP_BODY, httpResponseBuilt.getContent().toString(Charset.defaultCharset()));
    }

    private CookieWrapper createCookieWrapper(final String name,
                                              final String value,
                                              final String domain,
                                              final String path,
                                              final String expiryDate,
                                              final String secure,
                                              final String version)
    {
        final CookieWrapper cookieWrapper = new CookieWrapper();
        cookieWrapper.setName(name);
        cookieWrapper.setValue(value);
        cookieWrapper.setDomain(domain);
        cookieWrapper.setPath(path);
        cookieWrapper.setExpiryDate(expiryDate);
        cookieWrapper.setSecure(secure);
        cookieWrapper.setVersion(version);
        return cookieWrapper;
    }

    private Map<String, String> getHeaderCookie(final List<Entry<String, String>> headers)
    {
        final Map<String, String> cookies = new HashMap<String, String>();
        for (final Entry<String, String> header : headers)
        {
            if ("Set-Cookie".equals(header.getKey()))
            {
                cookies.put(header.getValue().split("=")[0], header.getValue());
            }
        }
        return cookies;
    }

    private HttpResponseBuilder createHttpResponseBuilder() throws InitialisationException
    {
        final HttpResponseBuilder httpResponseBuilder = new HttpResponseBuilder();
        httpResponseBuilder.setMuleContext(muleContext);
        httpResponseBuilder.initialise();
        return httpResponseBuilder;
    }

    private void validateHeaders(final List<Entry<String, String>> responseHeaders)
    {
        validateHeader(responseHeaders, "Cache-Control", "max-age=3600");
        validateHeader(responseHeaders, "Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
        validateHeader(responseHeaders, "Location", "http://localhost:8080");
    }

    private void validateHeader(final List<Entry<String, String>> headers,
                                final String headerName,
                                final String expectedValue)
    {
        for (final Entry<String, String> header : headers)
        {
            if (headerName.equals(header.getKey()))
            {
                assertEquals(expectedValue, header.getValue());
                return;
            }
        }

        fail(String.format("Didn't find header: %s=%s in headers: %s", headerName, expectedValue, headers));
    }

    private void mockParse()
    {
        when(mockExpressionManager.isExpression(anyString())).thenReturn(true);

        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
            new Answer<Object>()
            {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable
                {
                    return invocation.getArguments()[0];
                }
            });
    }

    private void acceptMockExpressions(final String... expressions)
    {
        for (final String expression : expressions)
        {
            when(mockExpressionManager.isExpression(expression)).thenReturn(true);
        }
    }
}

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

import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.PropertyScope;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.nio.tcp.TcpConnector;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.StringUtils;

/**
 * A {@link MuleMessageFactory} that builds {@link MuleMessage}s for each external
 * request received by the connector, either inbound or as a synchronous response to
 * an outbound interaction. As such it can deal with subclasses of
 * {@link StreamableHttpMessage} (the extended version of Netty's {@link HttpMessage}
 * ) which are {@link StreamableHttpRequest} and {@link StreamableHttpResponse}.
 */
public class HttpMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static final Log LOG = LogFactory.getLog(HttpMuleMessageFactory.class);
    private static final String DEFAULT_ENCODING = "UTF-8";
    
    private boolean enableCookies = false;
    private String cookieSpec;

    public HttpMuleMessageFactory(final MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return new Class[]{StreamableHttpMessage.class, WebSocketMessage.class};
    }

    @Override
    protected Object extractPayload(final Object o, final String encoding) throws Exception
    {
        if (o instanceof StreamableHttpMessage)
        {
            return extractPayload((StreamableHttpMessage) o, encoding);
        }
        else if (o instanceof WebSocketMessage)
        {
            return extractPayload((WebSocketMessage) o, encoding);
        }
        else
        {
            throw new MessageTypeNotSupportedException(o, this.getClass());
        }
    }

    @Override
    protected void addProperties(final DefaultMuleMessage message, final Object o) throws Exception
    {
        if (o instanceof StreamableHttpMessage)
        {
            addProperties(message, (StreamableHttpMessage) o);
        }
        else if (o instanceof WebSocketMessage)
        {
            addProperties(message, (WebSocketMessage) o);
        }
        else
        {
            throw new MessageTypeNotSupportedException(o, this.getClass());
        }
    }

    protected Object extractPayload(final WebSocketMessage webSocketMessage, final String encoding)
    {
        return webSocketMessage.getPayload();
    }

    protected void addProperties(final DefaultMuleMessage message, final WebSocketMessage webSocketMessage)
        throws Exception
    {
        final Map<String, Object> inboundProps = new HashMap<String, Object>();
        inboundProps.put(HttpConnector.HTTP_REQUEST_PROPERTY, webSocketMessage.getPath());
        inboundProps.put(HttpConnector.WEBSOCKET_VERSION_PROPERTY, webSocketMessage.getWebSocketVersion()
            .toString());
        message.addInboundProperties(inboundProps);

        addChannelInformation(webSocketMessage.getChannel(), message);
    }

    protected Object extractPayload(final StreamableHttpMessage httpMessage, final String encoding)
        throws Exception

    {
        final Object messagePayload = httpMessage.getPayload();

        if (httpMessage instanceof StreamableHttpRequest)
        {
            // If HTTP method is GET and there's no response, we use the request URI
            // as the payload.
            final StreamableHttpRequest httpRequest = (StreamableHttpRequest) httpMessage;
            if ((messagePayload == null) && (httpRequest.getMethod().equals(HttpMethod.GET)))
            {
                return httpRequest.getUri();
            }
            else
            {
                return messagePayload;
            }
        }
        else if (httpMessage instanceof StreamableHttpResponse)
        {
            return messagePayload;
        }
        else
        {
            throw new IllegalArgumentException("Unsupported message: " + httpMessage);
        }
    }

    protected void addProperties(final DefaultMuleMessage message, final StreamableHttpMessage o)
        throws Exception
    {
        final StreamableHttpMessage httpMessage = o;
        final StreamableHttpRequest httpRequest = getHttpRequest(httpMessage);
        final StreamableHttpResponse httpResponse = getHttpResponse(httpMessage);

        final List<Entry<String, String>> httpHeaders = getCurrentSource(httpMessage).getHeaders();
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Initial headers: " + httpHeaders);
        }

        Map<String, Object> headers = convertHeadersToMap(httpHeaders);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers as map: " + headers);
        }

        final HttpVersion httpVersion = getCurrentSource(httpMessage).getProtocolVersion();
        headers.put(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion.toString());
        headers.put(HttpConnector.HTTP_METHOD_PROPERTY, httpRequest.getMethod().getName());

        final String requestUri = httpRequest.getUri();
        headers.put(HttpConnector.HTTP_REQUEST_PROPERTY, requestUri);
        headers.put(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, StringUtils.substringBefore(requestUri, "?"));

        // Make any URI params available and inbound message headers
        final QueryStringDecoder queryStringDecoder = new QueryStringDecoder(requestUri);
        addUriParamsAsHeaders(headers, queryStringDecoder.getParameters());
        headers.put(HttpConnector.HTTP_QUERY_STRING, StringUtils.substringAfter(requestUri, "?"));

        if (enableCookies)
        {
            headers.put(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);
        }

        if (httpResponse != null)
        {
            headers.put(HttpConnector.HTTP_STATUS_PROPERTY,
                Integer.toString(httpResponse.getStatus().getCode()));
            headers.put(HttpConnector.HTTP_REASON_PHRASE_PROPERTY, httpResponse.getStatus().getReasonPhrase());
        }

        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers before multipart conversion: " + headers);
        }
        convertMultiPartHeaders(headers);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers after multipart conversion: " + headers);
        }

        rewriteConnectionAndKeepAliveHeaders(message, headers);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers after rewriting connection and keep-alive: " + headers);
        }

        headers = processIncomingHeaders(headers);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers after processing incoming headers: " + headers);
        }

        headers.put(HttpConnector.HTTP_HEADERS, new HashMap<String, Object>(headers));

        final String encoding = getEncoding(headers);

        final Map<String, Object> queryParameters = new HashMap<String, Object>();
        queryParameters.put(HttpConnector.HTTP_QUERY_PARAMS, processQueryParams(requestUri, encoding));

        message.addInboundProperties(headers);
        message.addInboundProperties(queryParameters);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers after adding inbound properties: " + headers);
        }

        addChannelInformation(httpMessage.getChannel(), message);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers after adding channel information: " + headers);
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug(HttpConnector.HTTP_REQUEST_PROPERTY + "="
                      + message.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
        }

        // The encoding is stored as message property. To avoid overriding it from
        // the message properties, it must be initialized last
        initEncoding(message, encoding);
        if (LOG.isTraceEnabled())
        {
            LOG.trace("Headers initializing encoding: " + headers);
            LOG.trace("Message after initializing encoding: " + message);
        }
    }

    protected StreamableHttpMessage getCurrentSource(final StreamableHttpMessage httpMessage)
    {
        final StreamableHttpRequest httpRequest = getHttpRequest(httpMessage);
        final StreamableHttpResponse httpResponse = getHttpResponse(httpMessage);

        return httpResponse != null ? httpResponse : httpRequest;
    }

    protected void addChannelInformation(final Channel channel, final DefaultMuleMessage message)
    {
        if (channel == null)
        {
            return;
        }

        message.setInboundProperty(TcpConnector.CHANNEL_ID_PROPERTY, channel.getId());

        final SocketAddress remoteAddress = channel.getRemoteAddress();
        if (remoteAddress != null)
        {
            message.setInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS, remoteAddress.toString());
        }
    }

    protected StreamableHttpResponse getHttpResponse(final StreamableHttpMessage httpMessage)
    {
        return (httpMessage instanceof StreamableHttpResponse) ? (StreamableHttpResponse) httpMessage : null;
    }

    protected StreamableHttpRequest getHttpRequest(final StreamableHttpMessage httpMessage)
    {
        return (httpMessage instanceof StreamableHttpRequest)
                                                             ? (StreamableHttpRequest) httpMessage
                                                             : ((StreamableHttpResponse) httpMessage).getStreamableHttpRequest();
    }

    protected Map<String, Object> processIncomingHeaders(final Map<String, Object> headers) throws Exception
    {
        final Map<String, Object> outHeaders = new HashMap<String, Object>();

        for (String headerName : headers.keySet())
        {
            final Object headerValue = headers.get(headerName);

            // fix Mule headers?
            if (headerName.startsWith("X-MULE"))
            {
                headerName = headerName.substring(2);
            }

            // accept header & value
            outHeaders.put(headerName, headerValue);
        }

        return outHeaders;
    }

    protected Map<String, Object> convertHeadersToMap(final List<Entry<String, String>> headers)
        throws URISyntaxException
    {
        @SuppressWarnings("unchecked")
        final Map<String, Object> headersMap = new CaseInsensitiveHashMap();

        for (final Entry<String, String> header : headers)
        {
            // Cookies are a special case because there may be more than one cookie.
            if (HttpConnector.HTTP_COOKIES_PROPERTY.equals(header.getKey())
                || HttpConstants.HEADER_COOKIE.equalsIgnoreCase(header.getKey()))
            {
                putCookieHeaderInMapAsAServer(headersMap, header);
            }
            else if (HttpConstants.HEADER_COOKIE_SET.equalsIgnoreCase(header.getKey()))
            {
                putCookieHeaderInMapAsAClient(headersMap, header);
            }
            else
            {
                if ((!StringUtils.equalsIgnoreCase(header.getKey(), HttpConstants.HEADER_CONTENT_TYPE))
                    && (headersMap.containsKey(header.getKey())))
                {
                    if (headersMap.get(header.getKey()) instanceof String)
                    {
                        // concat
                        headersMap.put(header.getKey(),
                            headersMap.get(header.getKey()) + "," + header.getValue());
                    }
                    else
                    {
                        // override
                        headersMap.put(header.getKey(), header.getValue());
                    }
                }
                else
                {
                    headersMap.put(header.getKey(), header.getValue());
                }
            }
        }
        return headersMap;
    }

    protected void putCookieHeaderInMapAsAClient(final Map<String, Object> headersMap,
                                                 final Entry<String, String> header)
        throws URISyntaxException
    {
        final Cookie[] newCookies = CookieHelper.parseCookiesAsAClient(header.getValue());
        final Object preExistentCookies = headersMap.get(HttpConstants.HEADER_COOKIE_SET);
        final Object mergedCookie = CookieHelper.putAndMergeCookie(preExistentCookies, newCookies);
        headersMap.put(HttpConstants.HEADER_COOKIE_SET, mergedCookie);
    }

    protected void putCookieHeaderInMapAsAServer(final Map<String, Object> headersMap,
                                                 final Entry<String, String> header)
        throws URISyntaxException
    {
        if (enableCookies)
        {
            final Cookie[] newCookies = CookieHelper.parseCookiesAsAServer(header.getValue());
            if (newCookies.length > 0)
            {
                final Object oldCookies = headersMap.get(HttpConnector.HTTP_COOKIES_PROPERTY);
                final Object mergedCookies = CookieHelper.putAndMergeCookie(oldCookies, newCookies);
                headersMap.put(HttpConnector.HTTP_COOKIES_PROPERTY, mergedCookies);
            }
        }
    }

    protected String getEncoding(final Map<String, Object> headers)
    {
        final String contentType = (String) headers.get(HttpConstants.HEADER_CONTENT_TYPE);
        return HttpUtils.extractCharset(contentType, DEFAULT_ENCODING);
    }

    protected void initEncoding(final MuleMessage message, final String encoding)
    {
        message.setEncoding(encoding);
    }

    protected void rewriteConnectionAndKeepAliveHeaders(final DefaultMuleMessage message,
                                                        final Map<String, Object> headers)
    {
        // REVIEW This has been added to preserve the inbound "Connection" header
        // and allow to optionally return it as-is in the response. This was maybe
        // the intention of the following code but it's hard to understand.
        //
        final String connection = (String) headers.get(HttpConstants.HEADER_CONNECTION);
        if (StringUtils.isNotBlank(connection))
        {
            message.addProperties(
                Collections.singletonMap(HttpConstants.HEADER_CONNECTION, (Object) connection),
                PropertyScope.INVOCATION);
        }

        // REVIEW The following code comes from the HTTP transport: its purpose is
        // really hard to grasp. Also was it done the way it is because of the lack
        // of property scopes in Mule 2? Maybe the above code is better then?
        String headerValue;
        if (!isHttp11(headers))
        {
            if (StringUtils.equalsIgnoreCase(connection, "close"))
            {
                headerValue = Boolean.FALSE.toString();
            }
            else
            {
                // REVIEW This would yield to keep-alive == true for HTTP/1.0, as the
                // "Connection" header would most likely not be present for such
                // requests
                headerValue = Boolean.TRUE.toString();
            }
        }
        else
        {
            // REVIEW Why just checking the mere presence of the "Connection" header
            // and not its value for HTTP/1.1 requests?
            headerValue = headers.get(HttpConstants.HEADER_CONNECTION) != null
                                                                              ? Boolean.TRUE.toString()
                                                                              : Boolean.FALSE.toString();
        }

        // REVIEW what is the meaning of "true/false" for the "Connection" header?
        headers.put(HttpConstants.HEADER_CONNECTION, headerValue);

        headers.put(HttpConstants.HEADER_KEEP_ALIVE, headerValue);
    }

    protected boolean isHttp11(final Map<String, Object> headers)
    {
        final String httpVersion = (String) headers.get(HttpConnector.HTTP_VERSION_PROPERTY);
        return !HttpConstants.HTTP10.equalsIgnoreCase(httpVersion);
    }

    protected void addUriParamsAsHeaders(final Map<String, Object> headers,
                                         final Map<String, List<String>> uriParams)
    {
        for (final Entry<String, List<String>> uriParam : uriParams.entrySet())
        {
            final List<String> values = uriParam.getValue();
            final Object value = values.isEmpty() ? StringUtils.EMPTY : values.get(values.size() - 1);
            headers.put(uriParam.getKey(), value);
        }
    }

    protected Map<String, Object> processQueryParams(final String uri, final String encoding)
        throws UnsupportedEncodingException
    {
        final Map<String, Object> httpParams = new HashMap<String, Object>();

        final int i = uri.indexOf("?");
        if (i > -1)
        {
            final String queryString = uri.substring(i + 1);
            for (final StringTokenizer st = new StringTokenizer(queryString, "&"); st.hasMoreTokens();)
            {
                final String token = st.nextToken();
                final int idx = token.indexOf('=');
                if (idx < 0)
                {
                    addQueryParamToMap(httpParams, unescape(token, encoding), null);
                }
                else if (idx > 0)
                {
                    addQueryParamToMap(httpParams, unescape(token.substring(0, idx), encoding),
                        unescape(token.substring(idx + 1), encoding));
                }
            }
        }

        return httpParams;
    }

    protected void addQueryParamToMap(final Map<String, Object> httpParams,
                                      final String key,
                                      final String value)
    {
        final Object existingValue = httpParams.get(key);
        if (existingValue == null)
        {
            httpParams.put(key, value);
        }
        else if (existingValue instanceof List)
        {
            @SuppressWarnings("unchecked")
            final List<String> list = (List<String>) existingValue;
            list.add(value);
        }
        else if (existingValue instanceof String)
        {
            final List<String> list = new ArrayList<String>();
            list.add((String) existingValue);
            list.add(value);
            httpParams.put(key, list);
        }
    }

    protected String unescape(final String escapedValue, final String encoding)
        throws UnsupportedEncodingException
    {
        if (escapedValue != null)
        {
            return URLDecoder.decode(escapedValue, encoding);
        }
        return escapedValue;
    }

    protected void convertMultiPartHeaders(final Map<String, Object> headers)
    {
        // template method
    }

    public void setEnableCookies(final boolean enableCookies)
    {
        this.enableCookies = enableCookies;
    }

    public void setCookieSpec(final String cookieSpec)
    {
        this.cookieSpec = cookieSpec;
    }
}

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

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.QueryStringEncoder;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.HttpUtils;
import org.mule.transport.nio.http.StreamableHttpRequest;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.util.ObjectUtils;
import org.mule.util.StringUtils;

/**
 * <code>ObjectToHttpRequest</code> transforms a MuleMessage into an
 * {@link HttpRequest}.
 */
// shut the warning off because how could we get the event otherwise?
@SuppressWarnings("deprecation")
public class ObjectToHttpRequest extends AbstractObjectToHttpMessage
{
    public ObjectToHttpRequest()
    {
        registerSourceType(DataTypeFactory.MULE_MESSAGE);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(OutputHandler.class));
        registerSourceType(DataTypeFactory.create(NullPayload.class));
        registerSourceType(DataTypeFactory.create(HttpRequest.class));
        registerSourceType(DataTypeFactory.create(Map.class));

        setReturnDataType(DataTypeFactory.create(StreamableHttpRequest.class));
    }

    @Override
    public Object transformMessage(final MuleMessage msg, final String outputEncoding)
        throws TransformerException
    {
        if (msg.getPayload() instanceof StreamableHttpRequest)
        {
            return msg.getPayload();
        }

        try
        {
            final String method = getHttpMethod(msg);
            final String httpVersion = getHttpVersion(msg);
            final String uri = getURI(msg);

            final StreamableHttpRequest httpRequest = new StreamableHttpRequest(
                HttpVersion.valueOf(httpVersion), HttpMethod.valueOf(method), getRequestPath(uri));

            if (HttpConstants.METHOD_GET.equals(method))
            {
                setupGetRequest(httpRequest, msg, outputEncoding);
            }
            else if (HttpConstants.METHOD_POST.equalsIgnoreCase(method))
            {
                setupPostRequest(httpRequest, msg, outputEncoding);
            }
            else if (HttpConstants.METHOD_PUT.equalsIgnoreCase(method))
            {
                setupPutRequest(httpRequest, msg, outputEncoding);
            }

            // used to allow the user to set HttpMethodParams
            HttpConnector.warnDeprecatedOutboundProperty(msg, HttpConnector.HTTP_PARAMS_PROPERTY);

            setHeaders(httpRequest, msg);

            return httpRequest;
        }
        catch (final Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected String getHttpMethod(final MuleMessage msg)
    {
        return HttpUtils.findProperty(msg, HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_POST,
            PropertyScope.OUTBOUND, PropertyScope.INVOCATION);
    }

    protected String getURI(final MuleMessage message) throws TransformerException
    {
        final String endpointAddress = message.getOutboundProperty(MuleProperties.MULE_ENDPOINT_PROPERTY,
            null);

        if (endpointAddress == null)
        {
            throw new TransformerException(
                HttpMessages.eventPropertyNotSetCannotProcessRequest(MuleProperties.MULE_ENDPOINT_PROPERTY),
                this);
        }

        return endpointAddress;
    }

    protected String getHttpVersion(final MuleMessage msg)
    {
        return msg.getOutboundProperty(HttpConnector.HTTP_VERSION_PROPERTY, HttpConstants.HTTP11);
    }

    protected void setupGetRequest(final HttpRequest httpRequest,
                                   final MuleMessage msg,
                                   final String outputEncoding) throws Exception
    {
        final String paramNameProperty = msg.getOutboundProperty(HttpConnector.HTTP_GET_BODY_PARAM_PROPERTY,
            null);

        if (paramNameProperty == null)
        {
            return;
        }

        final Object src = msg.getPayload();

        final URI uri = new URI(httpRequest.getUri());
        String query = uri.getRawQuery();

        final String paramName = URLEncoder.encode(paramNameProperty, outputEncoding);

        final Boolean encode = HttpUtils.findProperty(msg, HttpConnector.HTTP_ENCODE_PARAMVALUE,
            Boolean.TRUE, PropertyScope.INVOCATION, PropertyScope.OUTBOUND);
        final String paramValue = encode ? URLEncoder.encode(src.toString(), outputEncoding) : src.toString();

        if (query == null)
        {
            query = paramName + "=" + paramValue;
        }
        else
        {
            query += "&" + paramName + "=" + paramValue;
        }

        String newUri = uri.getPath();
        if (StringUtils.isNotBlank(query))
        {
            newUri = newUri + "?" + query;
        }

        final String fragment = uri.getFragment();
        if (StringUtils.isNotBlank(fragment))
        {
            newUri = newUri + "#" + fragment;
        }

        httpRequest.setUri(newUri);
    }

    protected void setupPostRequest(final StreamableHttpRequest httpRequest,
                                    final MuleMessage msg,
                                    final String outputEncoding) throws Exception
    {
        final String bodyParameterName = getBodyParameterName(msg);
        final Object src = msg.getPayload();
        final Charset charset = Charset.forName(outputEncoding);

        if (src instanceof Map)
        {
            final QueryStringEncoder qse = new QueryStringEncoder(StringUtils.EMPTY, charset);
            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) src).entrySet())
            {
                qse.addParam(entry.getKey().toString(), entry.getValue().toString());
            }
            setQueryString(httpRequest, charset, qse);
        }
        else if (bodyParameterName != null)
        {
            final QueryStringEncoder qse = new QueryStringEncoder(StringUtils.EMPTY, charset);
            qse.addParam(bodyParameterName, src.toString());
            setQueryString(httpRequest, charset, qse);
        }
        else
        {
            setRequestContent(src, outputEncoding, msg, httpRequest);
        }

        checkForContentTypeAndLength(msg, httpRequest);
    }

    protected void setupPutRequest(final StreamableHttpRequest httpRequest,
                                   final MuleMessage msg,
                                   final String outputEncoding) throws Exception
    {
        final Object payload = msg.getPayload();
        setRequestContent(payload, outputEncoding, msg, httpRequest);
        checkForContentTypeAndLength(msg, httpRequest);
    }

    protected void setQueryString(final StreamableHttpRequest httpRequest,
                                  final Charset charset,
                                  final QueryStringEncoder qse)
    {
        httpRequest.setContent(ChannelBuffers.copiedBuffer(StringUtils.removeStart(qse.toString(), "?"),
            charset));
        httpRequest.setHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.FORM_URL_ENCODED_CONTENT_TYPE);
    }

    protected String getBodyParameterName(final MuleMessage message)
    {
        String bodyParameter = message.getOutboundProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
        if (bodyParameter == null)
        {
            bodyParameter = message.getInvocationProperty(HttpConnector.HTTP_POST_BODY_PARAM_PROPERTY);
        }
        return bodyParameter;
    }

    protected void setRequestContent(final Object src,
                                     final String encoding,
                                     final MuleMessage msg,
                                     final StreamableHttpRequest httpRequest) throws Exception
    {
        // if we have attachments, go multipart
        if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
        {
            try
            {
                setMultiPartContent(msg, httpRequest, encoding);
                return;
            }
            catch (final Exception e)
            {
                throw new TransformerException(this, e);
            }
        }

        // don't set a POST payload if the body is a Null Payload.
        // This way client calls can control if a POST body is posted explicitly
        if ((msg.getPayload() instanceof NullPayload))
        {
            return;
        }

        String outboundContentType = (String) msg.getProperty(HttpConstants.HEADER_CONTENT_TYPE,
            PropertyScope.OUTBOUND);
        if (outboundContentType == null)
        {
            outboundContentType = (getEndpoint() != null ? getEndpoint().getMimeType() : null);
        }
        if (outboundContentType == null)
        {
            outboundContentType = HttpConstants.DEFAULT_CONTENT_TYPE;
            logger.info("Content-Type not set on outgoing request, defaulting to: " + outboundContentType);
        }

        if ((!StringUtils.equalsIgnoreCase(encoding, "UTF-8"))
            && (!StringUtils.containsIgnoreCase(outboundContentType, "charset")))
        {
            outboundContentType += "; charset=" + encoding;
        }

        httpRequest.setHeader(HttpConstants.HEADER_CONTENT_TYPE, outboundContentType);

        if (src instanceof String)
        {
            httpRequest.setContent(ChannelBuffers.copiedBuffer(src.toString(),
                Charset.forName(HttpUtils.extractCharset(outboundContentType, encoding))));
            return;
        }

        if (src instanceof InputStream)
        {
            HttpUtils.setInputStreamContent(httpRequest, (InputStream) src);
            return;
        }

        if (src instanceof byte[])
        {
            httpRequest.setContent(ChannelBuffers.wrappedBuffer((byte[]) src));
            return;
        }

        if (src instanceof OutputHandler)
        {
            final MuleEvent event = RequestContext.getEvent();
            httpRequest.setStreamingContent((OutputHandler) src, event);
            return;
        }

        final byte[] buffer = msg.getPayloadAsBytes();
        httpRequest.setContent(ChannelBuffers.wrappedBuffer(buffer));
        return;
    }

    protected void checkForContentTypeAndLength(final MuleMessage msg, final HttpRequest httpRequest)
    {
        // if a content type was specified on the endpoint, use it
        final String outgoingContentType = msg.getInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE);
        if (outgoingContentType != null)
        {
            httpRequest.setHeader(HttpConstants.HEADER_CONTENT_TYPE, outgoingContentType);
        }

        // if a content lenght wasn't specified elsewhere, add one if possible
        final String outgoingContentLength = msg.getInvocationProperty(HttpConstants.HEADER_CONTENT_LENGTH);
        if ((outgoingContentLength == null) && (httpRequest.getContent() != null))
        {
            httpRequest.setHeader(HttpConstants.HEADER_CONTENT_LENGTH, httpRequest.getContent()
                .readableBytes());
        }
    }

    protected void setHeaders(final HttpRequest httpRequest, final MuleMessage msg)
        throws TransformerException
    {
        for (String headerName : msg.getOutboundPropertyNames())
        {
            final String headerValue = ObjectUtils.getString(msg.getOutboundProperty(headerName), null);

            if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX))
            {
                // Define Mule headers a custom headers
                headerName = new StringBuilder(30).append("X-").append(headerName).toString();
                httpRequest.addHeader(headerName, headerValue);
            }
            else if (!HttpConstants.RESPONSE_HEADER_NAMES.containsKey(headerName)
                     && !HttpConnector.HTTP_INBOUND_PROPERTIES.contains(headerName)
                     && !HttpConnector.HTTP_COOKIES_PROPERTY.equals(headerName)
                     && !HttpConstants.HEADER_CONTENT_TYPE.equals(headerName))
            {
                httpRequest.addHeader(headerName, headerValue);
            }
        }

        final Object authorizationOutboundProperty = msg.getOutboundProperty(HttpConstants.HEADER_AUTHORIZATION);
        if ((authorizationOutboundProperty != null)
            && (httpRequest.getHeader(HttpConstants.HEADER_AUTHORIZATION) == null))
        {
            final String auth = msg.getOutboundProperty(HttpConstants.HEADER_AUTHORIZATION);
            httpRequest.setHeader(HttpConstants.HEADER_AUTHORIZATION, auth);
        }
    }
}

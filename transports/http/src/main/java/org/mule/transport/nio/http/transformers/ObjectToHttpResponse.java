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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.config.MuleManifest;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.CookieHelper;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.HttpUtils;
import org.mule.transport.nio.http.StreamableHttpResponse;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

/**
 * Converts a {@link MuleMessage} into an {@link HttpResponse}.
 */
@SuppressWarnings("deprecation")
public class ObjectToHttpResponse extends AbstractObjectToHttpMessage
{
    private String server;

    public ObjectToHttpResponse()
    {
        registerSourceType(DataTypeFactory.OBJECT);

        setReturnDataType(DataTypeFactory.create(StreamableHttpResponse.class));
    }

    @Override
    public void initialise() throws InitialisationException
    {
        // When running with the source code, Meta information is not set
        // so product name and version are not available, hence we hard code
        if (MuleManifest.getProductName() == null)
        {
            server = "Mule/SNAPSHOT";
        }
        else
        {
            server = MuleManifest.getProductName() + "/" + MuleManifest.getProductVersion();
        }
    }

    @Override
    public Object transformMessage(final MuleMessage msg, final String outputEncoding)
        throws TransformerException
    {
        Object src = msg.getPayload();

        // Note this transformer excepts Null as we must always return a result from
        // the HTTP connector if a response transformer is present
        if ((src instanceof NullPayload) && (msg.getOutboundAttachmentNames().isEmpty()))
        {
            src = StringUtils.EMPTY;
        }

        try
        {
            StreamableHttpResponse response;
            if (src instanceof StreamableHttpResponse)
            {
                response = (StreamableHttpResponse) src;
            }
            else if (src instanceof HttpResponse)
            {
                response = new StreamableHttpResponse((HttpResponse) src);
            }
            else
            {
                response = createResponse(src, outputEncoding, msg);
            }

            // Ensure there's a content type header
            if (!response.containsHeader(HttpConstants.HEADER_CONTENT_TYPE))
            {
                response.addHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
            }

            if (isStreaming(response))
            {
                response.setHeader(HttpConstants.HEADER_CONNECTION, "close");
            }

            // Ensure there's a content length or transfer encoding header
            else if ((!response.containsHeader(HttpConstants.HEADER_CONTENT_LENGTH))
                     && (!response.containsHeader(HttpConstants.HEADER_TRANSFER_ENCODING)))
            {
                if (response.getContent() != null)
                {
                    final long len = response.getContent().readableBytes();
                    response.setHeader(HttpConstants.HEADER_CONTENT_LENGTH, Long.toString(len));
                }
                else
                {
                    response.setHeader(HttpConstants.HEADER_CONTENT_LENGTH, "0");
                }
            }

            // See if the the client explicitly handles connection persistence
            final String connHeader = HttpUtils.findProperty(msg, HttpConstants.HEADER_CONNECTION,
                PropertyScope.OUTBOUND, PropertyScope.INVOCATION);
            if (StringUtils.equalsIgnoreCase(connHeader, "close"))
            {
                response.setHeader(HttpConstants.HEADER_CONNECTION, "close");
            }

            final String method = msg.getOutboundProperty(HttpConnector.HTTP_METHOD_PROPERTY);
            if ("HEAD".equalsIgnoreCase(method))
            {
                // this is a head request, we don't want to send the actual content
                // TODO set correct content-length header for chunked HEAD responses
                response.setContent(null);
            }
            return response;
        }
        catch (final Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

    protected boolean isStreaming(final HttpResponse httpResponse)
    {
        return httpResponse instanceof StreamableHttpResponse
               && ((StreamableHttpResponse) httpResponse).hasStreamingContent();
    }

    protected StreamableHttpResponse createResponse(final Object src,
                                                    final String encoding,
                                                    final MuleMessage msg)
        throws IOException, TransformerException
    {
        int status = HttpConstants.SC_OK;
        final Object statusProperty = msg.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        if (statusProperty != null)
        {
            status = NumberUtils.toInt(statusProperty);
        }
        else if (msg.getExceptionPayload() != null)
        {
            status = HttpConstants.SC_INTERNAL_SERVER_ERROR;
        }

        String version = msg.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY);
        if (version == null)
        {
            version = HttpConstants.HTTP11;
        }

        final StreamableHttpResponse response = new StreamableHttpResponse(HttpVersion.valueOf(version),
            HttpResponseStatus.valueOf(status));

        response.setHeader(HttpConstants.HEADER_SERVER, server);

        final String contentType = HttpUtils.findProperty(msg, HttpConstants.HEADER_CONTENT_TYPE,
            PropertyScope.INBOUND, PropertyScope.INVOCATION);
        if (contentType != null)
        {
            response.setHeader(HttpConstants.HEADER_CONTENT_TYPE, contentType);
        }

        final String date = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US).format(new Date());
        response.setHeader(HttpConstants.HEADER_DATE, date);

        final String etag = msg.getOutboundProperty(HttpConstants.HEADER_ETAG);
        if (etag != null)
        {
            response.setHeader(HttpConstants.HEADER_ETAG, etag);
        }

        final Collection<String> headerNames = new LinkedList<String>();
        headerNames.addAll(HttpConstants.RESPONSE_HEADER_NAMES.values());
        headerNames.addAll(HttpConstants.GENERAL_AND_ENTITY_HEADER_NAMES.values());
        for (final String headerName : headerNames)
        {
            if (HttpConstants.HEADER_COOKIE_SET.equals(headerName))
            {
                final Object cookiesObject = HttpUtils.findProperty(msg, headerName,
                    PropertyScope.INVOCATION, PropertyScope.OUTBOUND, PropertyScope.INBOUND);

                handleCookieObject(msg, response, headerName, cookiesObject);
            }
            else
            {
                final Object value = HttpUtils.findProperty(msg, headerName, PropertyScope.INVOCATION,
                    PropertyScope.OUTBOUND);

                if (value != null)
                {
                    response.setHeader(headerName, value);
                }
            }
        }

        // attach the outbound properties to the message
        // note that some may have already been attached in the previous loop that
        // focuses on standard HTTP response headers: they'll overriden here
        for (String headerName : msg.getOutboundPropertyNames())
        {
            final Object v = msg.getOutboundProperty(headerName);
            if (v != null)
            {
                if (headerName.startsWith(MuleProperties.PROPERTY_PREFIX))
                {
                    headerName = HttpConstants.CUSTOM_HEADER_PREFIX + headerName;
                }
                response.setHeader(headerName, v.toString());
            }
        }

        if (msg.getCorrelationId() != null)
        {
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_ID_PROPERTY, msg.getCorrelationId());
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                String.valueOf(msg.getCorrelationGroupSize()));
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                String.valueOf(msg.getCorrelationSequence()));
        }

        if (msg.getReplyTo() != null)
        {
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_REPLY_TO_PROPERTY,
                msg.getReplyTo().toString());
        }

        try
        {
            setBody(response, msg);
        }
        catch (final Exception e)
        {
            throw new TransformerException(this, e);
        }

        return response;
    }

    public void handleCookieObject(final MuleMessage msg,
                                   final StreamableHttpResponse response,
                                   final String headerName,
                                   final Object cookiesObject)
    {
        if (cookiesObject == null)
        {
            return;
        }

        if (!(cookiesObject instanceof Cookie[]))
        {
            response.addHeader(headerName, cookiesObject.toString());
            return;
        }

        final Cookie[] arrayOfCookies = CookieHelper.asArrayOfCookies(cookiesObject);
        for (final Cookie cookie : arrayOfCookies)
        {
            String cookieSpec = msg.getOutboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);

            if (endpoint != null)
            {
                final String endpointCookieSpec = (String) endpoint.getProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY);
                if (StringUtils.isNotBlank(endpointCookieSpec))
                {
                    cookieSpec = endpointCookieSpec;
                }
                else
                {
                    if (endpoint.getConnector() instanceof HttpConnector)
                    {
                        cookieSpec = ((HttpConnector) endpoint.getConnector()).getCookieSpec();
                    }
                }
            }

            if (StringUtils.isBlank(cookieSpec))
            {
                cookieSpec = HttpConnector.DEFAULT_COOKIE_SPEC;
            }

            msg.setOutboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);

            final int cookieVersion = HttpConnector.getCookieVersion(cookieSpec);
            response.addHeader(headerName,
                CookieHelper.formatCookieForASetCookieHeader(cookie, cookieVersion));
        }
    }

    protected void setBody(final StreamableHttpResponse response, final MuleMessage msg) throws Exception
    {
        if (msg == null)
        {
            return;
        }

        final Charset charset = getCharset(response, msg.getEncoding());

        // if we have attachments, go multipart
        if (msg.getOutboundAttachmentNames() != null && msg.getOutboundAttachmentNames().size() > 0)
        {
            try
            {
                setMultiPartContent(msg, response, charset.name());
                return;
            }
            catch (final Exception e)
            {
                throw new TransformerException(this, e);
            }
        }

        final Object payload = msg.getPayload();
        if (payload instanceof NullPayload)
        {
            return;
        }

        if (payload instanceof String)
        {
            response.setContent(ChannelBuffers.copiedBuffer((String) payload, charset));
        }
        else if (payload instanceof byte[])
        {
            response.setContent(ChannelBuffers.copiedBuffer((byte[]) payload));
        }
        else if (payload instanceof InputStream)
        {
            HttpUtils.setInputStreamContent(response, (InputStream) payload);
        }
        else
        {
            // no other way to get the event here :(
            final MuleEvent event = RequestContext.getEvent();
            response.setStreamingContent(msg.getPayload(DataTypeFactory.create(OutputHandler.class)), event);
        }
    }

    protected Charset getCharset(final HttpResponse response, final String fallbackCharset)
    {
        try
        {
            return Charset.forName(extractCharset(response, fallbackCharset));
        }
        catch (final RuntimeException re)
        {
            return Charset.defaultCharset();
        }
    }

    protected String extractCharset(final HttpResponse response, final String fallbackCharset)
    {
        final List<String> contentTypeHeaders = response.getHeaders(HttpConstants.HEADER_CONTENT_TYPE);
        for (final String contentTypeHeader : contentTypeHeaders)
        {
            final String charset = HttpUtils.extractCharset(contentTypeHeader, null);
            if (StringUtils.isNotBlank(charset))
            {
                return charset;
            }
        }
        return fallbackCharset;
    }

    @Override
    public boolean isAcceptNull()
    {
        return true;
    }
}

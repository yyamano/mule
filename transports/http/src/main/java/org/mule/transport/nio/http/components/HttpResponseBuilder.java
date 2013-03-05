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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.processor.AbstractMessageProcessorOwner;
import org.mule.transformer.AbstractTransformer;
import org.mule.transport.nio.http.CacheControlHeader;
import org.mule.transport.nio.http.CookieHelper;
import org.mule.transport.nio.http.CookieWrapper;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.StreamableHttpResponse;
import org.mule.transport.nio.http.transformers.ObjectToHttpResponse;
import org.mule.util.StringUtils;

public class HttpResponseBuilder extends AbstractMessageProcessorOwner
    implements Initialisable, MessageProcessor
{
    private List<CookieWrapper> cookieWrappers = new ArrayList<CookieWrapper>();
    private String contentType;
    private String status;
    private String version;
    private CacheControlHeader cacheControl;
    private boolean propagateMuleProperties = false;
    private AbstractTransformer bodyTransformer;
    private ObjectToHttpResponse objectToHttpResponse;

    private final Map<String, String> headers = new HashMap<String, String>();
    private final List<MessageProcessor> ownedMessageProcessor = new ArrayList<MessageProcessor>();

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        objectToHttpResponse = new ObjectToHttpResponse();
        objectToHttpResponse.setMuleContext(muleContext);
        objectToHttpResponse.initialise();
    }

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        final MuleMessage msg = event.getMessage();
        final HttpResponse httpResponse = createHttpResponse(msg, event);

        propagateMessageProperties(httpResponse, msg);
        setHttpProtocolVersion(httpResponse, msg);
        setStatus(httpResponse, event);
        setContentType(httpResponse, event);
        setHeaders(httpResponse, event);
        setCookies(httpResponse, event);
        setCacheControl(httpResponse, event);
        final String date = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US).format(new Date());
        httpResponse.setHeader(HttpConstants.HEADER_DATE, date);

        return event;
    }

    protected HttpResponse createHttpResponse(final MuleMessage msg, final MuleEvent event)
        throws MuleException, TransformerException
    {
        if (bodyTransformer != null)
        {
            msg.applyTransformers(event, bodyTransformer);
        }

        final StreamableHttpResponse httpResponse = (StreamableHttpResponse) objectToHttpResponse.transformMessage(
            msg, event.getEncoding());
        msg.setPayload(httpResponse);
        return httpResponse;
    }

    @Override
    protected List<MessageProcessor> getOwnedMessageProcessors()
    {
        return ownedMessageProcessor;
    }

    private void propagateMessageProperties(final HttpResponse response, final MuleMessage message)
    {
        copyOutboundProperties(response, message);
        if (propagateMuleProperties)
        {
            copyCorrelationIdProperties(response, message);
            copyReplyToProperty(response, message);
        }
    }

    private void copyCorrelationIdProperties(final HttpResponse response, final MuleMessage message)
    {
        if (message.getCorrelationId() != null)
        {
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_ID_PROPERTY, message.getCorrelationId());
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                String.valueOf(message.getCorrelationGroupSize()));
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX
                               + MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                String.valueOf(message.getCorrelationSequence()));
        }
    }

    private void copyReplyToProperty(final HttpResponse response, final MuleMessage message)
    {
        if (message.getReplyTo() != null)
        {
            response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX + MuleProperties.MULE_REPLY_TO_PROPERTY,
                message.getReplyTo().toString());
        }
    }

    protected void copyOutboundProperties(final HttpResponse response, final MuleMessage message)
    {
        for (final String headerName : message.getOutboundPropertyNames())
        {
            final Object headerValue = message.getOutboundProperty(headerName);
            if (headerValue != null)
            {
                if (isMuleProperty(headerName))
                {
                    if (propagateMuleProperties)
                    {
                        addMuleHeader(response, headerName, headerValue);
                    }
                }
                else if (isMultiValueCookie(headerName, headerValue))
                {
                    addMultiValueCookie(response, (Cookie[]) headerValue);
                }
                else
                {
                    response.setHeader(headerName, headerValue.toString());
                }
            }
        }
    }

    private void addMuleHeader(final HttpResponse response, final String headerName, final Object headerValue)
    {
        response.setHeader(HttpConstants.CUSTOM_HEADER_PREFIX + headerName, headerValue.toString());
    }

    private boolean isMuleProperty(final String headerName)
    {
        return headerName.startsWith(MuleProperties.PROPERTY_PREFIX);
    }

    private boolean isMultiValueCookie(final String headerName, final Object headerValue)
    {
        return HttpConstants.HEADER_COOKIE_SET.equals(headerName) && headerValue instanceof Cookie[];
    }

    protected void setCacheControl(final HttpResponse response, final MuleEvent event)
    {
        if (cacheControl == null)
        {
            return;
        }

        cacheControl.parse(event);
        String cacheControlValue = cacheControl.toString();
        if (StringUtils.isBlank(cacheControlValue))
        {
            return;
        }

        if (headers.get(HttpConstants.HEADER_CACHE_CONTROL) != null)
        {
            final String cacheControlHeader = response.getHeader(HttpConstants.HEADER_CACHE_CONTROL);
            if (cacheControlHeader != null)
            {
                cacheControlValue += "," + cacheControlHeader;
            }
        }
        response.setHeader(HttpConstants.HEADER_CACHE_CONTROL, cacheControlValue);
    }

    private void addMultiValueCookie(final HttpResponse response, final Cookie[] cookies)
    {
        final Cookie[] arrayOfCookies = CookieHelper.asArrayOfCookies(cookies);
        for (final Cookie cookie : arrayOfCookies)
        {
            response.addHeader(HttpConstants.HEADER_COOKIE_SET,
                CookieHelper.formatCookieForASetCookieHeader(cookie));
        }
    }

    protected void setCookies(final HttpResponse response, final MuleEvent event) throws MuleException
    {
        final int defaultCookieVersion = getCookieVersion(event.getMessage());

        for (final CookieWrapper cookieWrapper : cookieWrappers)
        {
            try
            {
                cookieWrapper.parse(event);
                final Cookie cookie = cookieWrapper.createCookie(defaultCookieVersion);
                response.addHeader(HttpConstants.HEADER_COOKIE_SET,
                    CookieHelper.formatCookieForASetCookieHeader(cookie));
            }
            catch (final Exception e)
            {
                throw new DefaultMuleException("Failed to set cookie wrapper: " + cookieWrapper, e);
            }
        }
    }

    protected int getCookieVersion(final MuleMessage message)
    {
        return HttpConnector.getCookieVersion(StringUtils.defaultIfEmpty(
            (String) message.getOutboundProperty(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY),
            HttpConnector.DEFAULT_COOKIE_SPEC));
    }

    protected void setHeaders(final HttpResponse response, final MuleEvent event)
    {
        if ((headers == null) || (headers.isEmpty()))
        {
            return;
        }

        for (final String headerName : headers.keySet())
        {
            final String name = parse(headerName, event);
            final String value = headers.get(headerName);
            if (HttpConstants.HEADER_EXPIRES.equals(name))
            {
                response.setHeader(name, evaluateDate(value, event));
            }
            else
            {
                response.setHeader(name, parse(value, event));
            }
        }
    }

    protected void setHttpProtocolVersion(final HttpResponse response, final MuleMessage message)
    {
        version = StringUtils.defaultIfEmpty(
            (String) message.getInboundProperty(HttpConnector.HTTP_VERSION_PROPERTY), HttpConstants.HTTP11);

        response.setProtocolVersion(HttpVersion.valueOf(version));
    }

    private void setStatus(final HttpResponse response, final MuleEvent event) throws MuleException
    {
        if (status != null)
        {
            response.setStatus(HttpResponseStatus.valueOf(Integer.valueOf(parse(status, event))));
        }
    }

    protected void setContentType(final HttpResponse response, final MuleEvent event)
    {
        if (contentType == null)
        {
            contentType = getDefaultContentType(event.getMessage());
        }
        response.setHeader(HttpConstants.HEADER_CONTENT_TYPE, parse(contentType, event));
    }

    private String parse(final String value, final MuleEvent event)
    {
        final ExpressionManager expressionManager = muleContext.getExpressionManager();

        if (StringUtils.isNotBlank(value) && (expressionManager.isExpression(value)))
        {
            return expressionManager.parse(value, event);
        }
        return value;
    }

    private String evaluateDate(final String value, final MuleEvent event)
    {
        Object realValue = value;
        final ExpressionManager expressionManager = muleContext.getExpressionManager();

        if (StringUtils.isNotBlank(value) && (expressionManager.isExpression(value)))
        {
            realValue = expressionManager.evaluate(value, event);
        }

        if (realValue instanceof Date)
        {
            // SimpleDateFormat is not threadsafe and must be created for each call
            final SimpleDateFormat dateFormatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
            dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormatter.format(realValue);
        }

        return String.valueOf(realValue);
    }

    private String getDefaultContentType(final MuleMessage message)
    {
        String contentType = message.getInboundProperty(HttpConstants.HEADER_CONTENT_TYPE);
        if (contentType == null)
        {
            contentType = HttpConstants.DEFAULT_CONTENT_TYPE;
        }
        return contentType;
    }

    public void setHeaders(final Map<String, String> headers)
    {
        this.headers.putAll(headers);
    }

    public void setStatus(final String status)
    {
        this.status = status;
    }

    public void setContentType(final String contentType)
    {
        this.contentType = contentType;
    }

    public void setVersion(final String version)
    {
        this.version = version;
    }

    public void setCookies(final List<CookieWrapper> cookies)
    {
        this.cookieWrappers = cookies;
    }

    public void addHeader(final String key, final String value)
    {
        headers.put(key, value);
    }

    public void setCacheControl(final CacheControlHeader cacheControl)
    {
        this.cacheControl = cacheControl;
    }

    public String getVersion()
    {
        return version;
    }

    public void setPropagateMuleProperties(final boolean propagateMuleProperties)
    {
        this.propagateMuleProperties = propagateMuleProperties;
    }

    public void setMessageProcessor(final MessageProcessor messageProcessor)
    {
        this.bodyTransformer = (AbstractTransformer) messageProcessor;
        ownedMessageProcessor.add(bodyTransformer);
    }
}

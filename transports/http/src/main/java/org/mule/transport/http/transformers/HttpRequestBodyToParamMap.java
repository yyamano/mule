/*
 * $Id: HttpRequestBodyToParamMap.java 23948 2012-03-06 13:42:19Z evangelinamrm $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.net.URLCodec;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.util.StringUtils;

public class HttpRequestBodyToParamMap extends AbstractMessageTransformer
{
    public HttpRequestBodyToParamMap()
    {
        registerSourceType(DataTypeFactory.OBJECT);
        setReturnDataType(DataTypeFactory.create(Map.class));
    }

    @Override
    public Object transformMessage(final MuleMessage message, final String outputEncoding)
        throws TransformerException
    {
        final Map<String, Object> paramMap = new HashMap<String, Object>();

        try
        {
            final String httpMethod = message.getInboundProperty("http.method");
            final String contentType = message.getInboundProperty("Content-Type");

            final boolean isGet = HttpConstants.METHOD_GET.equalsIgnoreCase(httpMethod);
            final boolean isPost = HttpConstants.METHOD_POST.equalsIgnoreCase(httpMethod);
            boolean isUrlEncoded = false;
            if (contentType != null)
            {
                isUrlEncoded = contentType.startsWith("application/x-www-form-urlencoded");
            }

            if (!(isGet || (isPost && isUrlEncoded)))
            {
                throw new Exception("The HTTP method or content type is unsupported!");
            }

            String queryString = null;
            if (isGet)
            {
                final URI uri = new URI(message.getPayloadAsString(outputEncoding));
                queryString = uri.getRawQuery();
            }
            else if (isPost)
            {
                queryString = new String(message.getPayloadAsBytes());
            }

            if (StringUtils.isNotBlank(queryString))
            {
                addQueryStringToParameterMap(queryString, paramMap, outputEncoding);
            }
        }
        catch (final Exception e)
        {
            throw new TransformerException(this, e);
        }

        return paramMap;
    }

    protected void addQueryStringToParameterMap(final String queryString,
                                                final Map<String, Object> paramMap,
                                                final String outputEncoding) throws Exception
    {
        final String[] pairs = queryString.split("&");
        for (final String pair : pairs)
        {
            final String[] nameValue = pair.split("=");
            if (nameValue.length == 2)
            {
                final URLCodec codec = new URLCodec(outputEncoding);
                final String key = codec.decode(nameValue[0]);
                final String value = codec.decode(nameValue[1]);
                addToParameterMap(paramMap, key, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addToParameterMap(final Map<String, Object> paramMap, final String key, final String value)
    {
        final Object existingValue = paramMap.get(key);
        if (existingValue != null)
        {
            List<Object> values;
            if (existingValue instanceof List<?>)
            {
                // build new ArrayList to garantee mutability
                values = new ArrayList<Object>((List<Object>) existingValue);
            }
            else
            {
                // ditto
                values = new ArrayList<Object>(Collections.singleton(existingValue));
            }

            values.add(value);
            paramMap.put(key, values);
        }
        else
        {
            paramMap.put(key, value);
        }
    }

    @Override
    public boolean isAcceptNull()
    {
        return false;
    }
}

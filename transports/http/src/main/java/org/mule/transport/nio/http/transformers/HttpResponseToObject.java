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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.mule.DefaultMuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConstants;

/**
 * <code>HttpClientMethodResponseToObject</code> transforms a http client response to
 * a DefaultMuleMessage.
 */

public class HttpResponseToObject extends AbstractTransformer
{

    public HttpResponseToObject()
    {
        registerSourceType(DataTypeFactory.create(HttpResponse.class));
        setReturnDataType(DataTypeFactory.MULE_MESSAGE);
    }

    @Override
    public Object doTransform(final Object src, final String encoding) throws TransformerException
    {
        final HttpResponse httpResponse = (HttpResponse) src;

        // no way to retrieve further chunks from here :(
        if (httpResponse.isChunked())
        {
            throw new TransformerException(this, new UnsupportedOperationException(
                "Chunked messages can't be transformed"));
        }

        // content is either null or a byte array
        final ChannelBuffer content = httpResponse.getContent();
        final boolean noContent = content == ChannelBuffers.EMPTY_BUFFER;
        final Object msg = noContent ? NullPayload.getInstance() : content.readBytes(content.readableBytes())
            .array();

        // Standard headers
        final Map<String, Object> headerProps = new HashMap<String, Object>();

        for (final Entry<String, String> header : httpResponse.getHeaders())
        {
            String name = header.getKey();
            if (name.startsWith(HttpConstants.X_PROPERTY_PREFIX))
            {
                name = name.substring(2);
            }
            headerProps.put(name, header.getValue());
        }

        return new DefaultMuleMessage(msg, headerProps, muleContext);
    }
}

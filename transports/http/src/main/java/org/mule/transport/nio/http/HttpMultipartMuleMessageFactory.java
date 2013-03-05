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

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataHandler;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transformer.simple.ObjectToInputStream;
import org.mule.transport.http.multipart.MultiPartInputStream;
import org.mule.transport.http.multipart.Part;
import org.mule.transport.http.multipart.PartDataSource;
import org.mule.util.StringUtils;

/**
 * An HTTP-multipart aware {@link MuleMessageFactory}.
 */
public class HttpMultipartMuleMessageFactory extends HttpMuleMessageFactory
{
    private static final String PAYLOAD_PART_NAME = "payload";

    private final ObjectToInputStream otis;

    private final ThreadLocal<Collection<Part>> parts = new ThreadLocal<Collection<Part>>();

    public HttpMultipartMuleMessageFactory(final MuleContext context)
    {
        super(context);

        otis = new ObjectToInputStream();
        otis.setMuleContext(context);
    }

    @Override
    protected Object extractPayload(final StreamableHttpMessage httpMessage, final String encoding)
        throws Exception

    {
        final String contentType = getCurrentSource(httpMessage).getHeader(HttpConstants.HEADER_CONTENT_TYPE);
        final Object body = super.extractPayload(httpMessage, encoding);

        if (!StringUtils.containsIgnoreCase(contentType, HttpConstants.MULTIPART_FORM_DATA_CONTENT_TYPE))
        {
            return body;
        }

        final InputStream is = body instanceof InputStream
                                                          ? (InputStream) body
                                                          : (InputStream) otis.transform(body);

        final MultiPartInputStream in = new MultiPartInputStream(is, contentType, null);

        // We need to store this so that the headers for the part can be read
        parts.set(in.getParts());
        for (final Part part : parts.get())
        {
            if (part.getName().equals(PAYLOAD_PART_NAME))
            {
                final InputStream payloadPartInputStream = part.getInputStream();
                if (payloadPartInputStream != null)
                {
                    return payloadPartInputStream;
                }
            }
        }

        throw new IllegalArgumentException("no part named \"payload\" found");
    }

    @Override
    protected void addAttachments(final DefaultMuleMessage message, final Object transportMessage)
        throws Exception
    {
        if (parts.get() == null)
        {
            return;
        }

        try
        {
            for (final Part part : parts.get())
            {
                if (!part.getName().equals(PAYLOAD_PART_NAME))
                {
                    message.addInboundAttachment(part.getName(), new DataHandler(new PartDataSource(part)));
                }
            }
        }
        finally
        {
            // Attachments are the last thing to get processed
            parts.remove();
        }
    }

    @Override
    protected void convertMultiPartHeaders(final Map<String, Object> headers)
    {
        if (parts.get() == null)
        {
            return;
        }
        for (final Part part : parts.get())
        {
            if (part.getName().equals(PAYLOAD_PART_NAME))
            {
                for (final String name : part.getHeaderNames())
                {
                    headers.put(name, part.getHeader(name));
                }
                break;
            }
        }
    }
}

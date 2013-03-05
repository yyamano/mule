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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jboss.netty.handler.codec.http.HttpResponse;
import org.mule.api.MuleContext;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.mule.transformer.simple.ObjectToString;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.nio.http.StreamableHttpResponse;
import org.mule.util.StringUtils;

/**
 * Converts an Http Response object to String. Note that the response headers are
 * preserved.
 */
public class HttpResponseToString extends AbstractTransformer
{
    public static final String CRLF = "\r\n";

    private final ObjectToString objectToStringTransformer;

    public HttpResponseToString()
    {
        registerSourceType(DataTypeFactory.create(HttpResponse.class));
        setReturnDataType(DataTypeFactory.STRING);

        objectToStringTransformer = new ObjectToString();
    }

    @Override
    public void setMuleContext(final MuleContext context)
    {
        super.setMuleContext(context);
        objectToStringTransformer.setMuleContext(context);
    }

    /**
     * Perform the transformation to always return a String object
     */
    @Override
    protected Object doTransform(final Object src, final String encoding) throws TransformerException
    {
        final HttpResponse httpResponse = (HttpResponse) src;

        if ((httpResponse.isChunked()) && (!(src instanceof StreamableHttpResponse)))

        {
            throw new TransformerException(this, new UnsupportedOperationException(
                "Chunked " + HttpResponse.class.getName() + " can't be transformed to String"));
        }

        String payloadAsString = StringUtils.EMPTY;

        if (src instanceof StreamableHttpResponse)
        {
            payloadAsString = (String) objectToStringTransformer.doTransform(
                ((StreamableHttpResponse) src).getPayload(), encoding);
        }
        else
        {
            payloadAsString = httpResponse.getContent().toString(Charset.forName(encoding));
        }

        final StringWriter stringWriter = new StringWriter();
        final PrintWriter writer = new PrintWriter(stringWriter);

        writer.print(httpResponse.getProtocolVersion().toString());
        writer.print(" ");
        writer.print(httpResponse.getStatus().toString());
        writer.print(CRLF);

        // sort headers by name
        final SortedSet<String> headerNames = new TreeSet<String>(httpResponse.getHeaderNames());
        for (final String headerName : headerNames)
        {
            // rely on List.toString() to render multiple header values
            final List<String> headerValues = httpResponse.getHeaders(headerName);
            for (final String headerValue : headerValues)
            {
                writer.printf("%s: %s", headerName, headerValue);
                writer.print(CRLF);
            }
        }

        writer.print(CRLF);

        if (httpResponse.isChunked())
        {
            // dump the whole content in one chunk
            try
            {
                writer.print(Integer.toHexString(payloadAsString.getBytes(encoding).length));
            }
            catch (final UnsupportedEncodingException uee)
            {
                throw new TransformerException(this, uee);
            }
            writer.print(CRLF);
            writer.print(payloadAsString);
            writer.print(CRLF);
            writer.print(0);
            writer.print(CRLF);
            writer.print(CRLF);
        }
        else
        {
            writer.print(payloadAsString);
        }

        writer.close();
        return stringWriter.toString();
    }
}

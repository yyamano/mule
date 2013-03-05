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

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.junit.Test;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.StreamableHttpResponse;

public class HttpResponseToStringTestCase extends AbstractMuleContextTestCase
{
    private final String statusLine = "HTTP/1.1 200 OK";
    private final String headerCT = "Content-Type: text/plain";
    private final String headerTE = "Transfer-Encoding: chunked";
    private final String contentLength = "Content-Length: ";
    private final String body = "<html><head></head><body><p>WOW</p></body></html>";

    private String resultChunked = statusLine + HttpResponseToString.CRLF + contentLength + body.length()
                                   + HttpResponseToString.CRLF + headerCT + HttpResponseToString.CRLF
                                   + headerTE + HttpResponseToString.CRLF + HttpResponseToString.CRLF;
    private String resultNotChunked = statusLine + HttpResponseToString.CRLF + contentLength + body.length()
                                      + HttpResponseToString.CRLF + headerCT + HttpResponseToString.CRLF
                                      + HttpResponseToString.CRLF;

    private HttpResponseToString transformer;
    private HttpResponse response;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        transformer = new HttpResponseToString();

        response = new StreamableHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
        response.setHeader(HttpConstants.HEADER_CONTENT_LENGTH, body.length());
        response.setContent(ChannelBuffers.copiedBuffer(body, Charset.defaultCharset()));
    }

    /**
     * Check consistency of the transformed {@link HttpResponse} string when HTTP
     * transfer encoding is chunked
     * 
     * @throws Exception
     */
    @Test
    public void testTransformChunked() throws Exception
    {
        response = new StreamableHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
        {
            @Override
            public Object getPayload()
            {
                return new ByteArrayInputStream(body.getBytes());
            }
        };
        response.setChunked(true);
        response.setHeader(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.DEFAULT_CONTENT_TYPE);
        response.setHeader(HttpConstants.HEADER_CONTENT_LENGTH, body.length());

        transformer.setReturnDataType(DataTypeFactory.STRING);

        response.setHeader(HttpConstants.HEADER_TRANSFER_ENCODING, HttpConstants.TRANSFER_ENCODING_CHUNKED);
        resultChunked += "31\r\n" + body + "\r\n0\r\n\r\n";

        final String result = (String) transformer.doTransform(response, "ISO-8859-1");

        assertEquals(resultChunked, result);
    }

    /**
     * Check consistency of the transformed {@link HttpResponse} string when HTTP
     * transfer encoding is chunked
     * 
     * @throws Exception
     */
    @Test
    public void testTransformNotChunked() throws Exception
    {
        resultNotChunked += body;

        final String result = (String) transformer.doTransform(response, "ISO-8859-1");

        assertEquals(resultNotChunked, result);
    }

    /**
     * Expect a {@link TransformerException} when the encoding is not supported.
     * 
     * @throws Exception
     */
    @Test(expected = TransformerException.class)
    public void testTransformException() throws Exception
    {
        transformer.doTransform(response, "ISO-8859-20");
    }
}

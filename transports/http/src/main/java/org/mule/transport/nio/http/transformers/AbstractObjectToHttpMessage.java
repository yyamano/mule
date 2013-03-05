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
import java.net.URI;
import java.net.URISyntaxException;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.activation.URLDataSource;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.mule.api.MuleMessage;
import org.mule.message.ds.StringDataSource;
import org.mule.transformer.AbstractMessageTransformer;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.http.multipart.MultiPartOutputStream;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

/**
 * Provides common functionalities to {@link ObjectToHttpRequest} and
 * {@link ObjectToHttpResponse} transformers.
 */
public abstract class AbstractObjectToHttpMessage extends AbstractMessageTransformer
{
    protected void setMultiPartContent(final MuleMessage msg,
                                       final HttpMessage httpRequest,
                                       final String outputEncoding) throws Exception
    {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final MultiPartOutputStream multiPartOutputStream = new MultiPartOutputStream(baos, outputEncoding);

        if (!(msg.getPayload() instanceof NullPayload))
        {
            startFilePart(multiPartOutputStream, "payload");
            baos.write(msg.getPayloadAsBytes());
        }

        for (final String name : msg.getOutboundAttachmentNames())
        {
            String fileName = name;
            final DataHandler dh = msg.getOutboundAttachment(name);
            if (dh.getDataSource() instanceof StringDataSource)
            {
                final StringDataSource ds = (StringDataSource) dh.getDataSource();
                startStringPart(multiPartOutputStream, ds.getName());
                IOUtils.copy(ds.getInputStream(), multiPartOutputStream);
            }
            else
            {
                if (dh.getDataSource() instanceof FileDataSource)
                {
                    fileName = ((FileDataSource) dh.getDataSource()).getFile().getName();
                }
                else if (dh.getDataSource() instanceof URLDataSource)
                {
                    fileName = ((URLDataSource) dh.getDataSource()).getURL().getFile();
                    // Don't use the whole file path, just the file name
                    final int x = fileName.lastIndexOf("/");
                    if (x > -1)
                    {
                        fileName = fileName.substring(x + 1);
                    }
                }
                startFilePart(multiPartOutputStream, dh.getName(), fileName, dh.getContentType());
                IOUtils.copy(dh.getInputStream(), multiPartOutputStream);
            }
        }

        multiPartOutputStream.flush();
        multiPartOutputStream.close();

        httpRequest.setContent(ChannelBuffers.wrappedBuffer(baos.toByteArray()));
        httpRequest.setHeader(
            HttpConstants.HEADER_CONTENT_TYPE,
            HttpConstants.MULTIPART_FORM_DATA_CONTENT_TYPE + "; boundary="
                            + multiPartOutputStream.getBoundary());
    }

    protected void startStringPart(final MultiPartOutputStream multiPartOutputStream, final String name)
        throws IOException
    {
        multiPartOutputStream.startPart("text/plain", new String[]{"Content-Disposition: form-data; name=\""
                                                                   + name + "\""});
    }

    protected void startFilePart(final MultiPartOutputStream multiPartOutputStream, final String fileName)
        throws IOException
    {
        startFilePart(multiPartOutputStream, fileName, "application/octet-stream");
    }

    protected void startFilePart(final MultiPartOutputStream multiPartOutputStream,
                                 final String fileName,
                                 final String contentType) throws IOException
    {
        startFilePart(multiPartOutputStream, fileName, fileName, contentType);
    }

    protected void startFilePart(final MultiPartOutputStream multiPartOutputStream,
                                 final String name,
                                 final String fileName,
                                 final String contentType) throws IOException
    {
        multiPartOutputStream.startPart(contentType, new String[]{"Content-Disposition: form-data; name=\""
                                                                  + name + "\"; filename=\"" + fileName
                                                                  + "\""});
    }

    protected String getRequestPath(final String uriString) throws URISyntaxException
    {
        final URI uri = new URI(uriString);
        String requestPath = StringUtils.defaultString(uri.getPath(), "/");

        final String query = uri.getRawQuery();
        if (StringUtils.isNotBlank(query))
        {
            requestPath = requestPath + "?" + query;
        }

        final String fragment = uri.getFragment();
        if (StringUtils.isNotBlank(fragment))
        {
            requestPath = requestPath + "#" + fragment;
        }

        return requestPath.startsWith("/") ? requestPath : "/" + requestPath;
    }
}

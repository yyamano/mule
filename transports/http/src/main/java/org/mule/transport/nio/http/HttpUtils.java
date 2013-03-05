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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.HttpHeaders.Names;
import org.jboss.netty.handler.codec.http.HttpHeaders.Values;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

/**
 * Supporting utility methods.
 */
public abstract class HttpUtils
{
    private HttpUtils()
    {
        throw new UnsupportedOperationException("do not instantiate");
    }

    public static <T> T findProperty(final MuleMessage message,
                                     final String name,
                                     final T defaultValue,
                                     final PropertyScope... scopes)
    {
        for (final PropertyScope scope : scopes)
        {
            @SuppressWarnings("unchecked")
            final T value = (T) message.getProperty(name, scope);
            if (value != null)
            {
                return value;
            }
        }
        return defaultValue;
    }

    @SuppressWarnings("unchecked")
    public static <T> T findProperty(final MuleMessage message,
                                     final String name,
                                     final PropertyScope... scopes)
    {
        return (T) findProperty(message, name, null, scopes);
    }

    public static String extractCharset(final String contentType, final String defaultCharset)
    {
        if (StringUtils.isNotBlank(contentType))
        {
            for (final String contentTypePart : StringUtils.splitAndTrim(contentType, ";"))
            {
                final String[] contentTypePartElements = StringUtils.splitAndTrim(contentTypePart, "=");
                if ((contentTypePartElements.length == 2)
                    && (contentTypePartElements[0].equalsIgnoreCase("charset")))
                {
                    return contentTypePartElements[1];
                }
            }
        }
        return defaultCharset;
    }

    public static boolean hasProtocolOlderThan(final HttpMessage httpMessage, final HttpVersion version)
    {
        return httpMessage.getProtocolVersion().compareTo(version) < 0;
    }

    public static void setInputStreamContent(final StreamableHttpMessage httpMessage, final InputStream is)
        throws IOException
    {
        final OutputHandler oh = new OutputHandler()
        {
            public void write(final MuleEvent ignored, final OutputStream out) throws IOException
            {
                IOUtils.copy(is, out);
            }
        };
        // ok to pass a null event because the above output handler ignores it
        httpMessage.setStreamingContent(oh, null);
    }
    
    /**
     * logic is to look first for things that specifically say to close the connection (connection headers, config) 
     * and if they say close, then close the connection. If not decide if the connection should stay alive by looking at headers,
     * config and then the http version's default.
     * @param response
     * @return
     */
    public static boolean shouldClose(HttpMessage response, Boolean keepAliveConfig)
    {
        
        // does the header say to close the connection?
        String connection = response.getHeader(Names.CONNECTION);
        boolean headerHasKeepAliveValue = connection != null;
        if (Values.CLOSE.equalsIgnoreCase(connection))
        {
            return true;
        }

        // does the config say to close the connection?
        boolean endpointHasKeepAliveValue = (keepAliveConfig != null);
        if (endpointHasKeepAliveValue && !keepAliveConfig.booleanValue())
        {
            return true;
        }
        
        // if specified, take the value in the header, config or finally, the default version.
        if (headerHasKeepAliveValue || endpointHasKeepAliveValue)
        {
            return false;
        }
        else
        {
            return !HttpHeaders.isKeepAlive(response);
        }
    }
    


    /**
     * From org.jboss.netty.handler.codec.http.HttpHeaders except 'keep-alive' is with capitals.
     * Sets the value of the {@code "Connection"} header depending on the
     * protocol version of the specified message.  This method sets or removes
     * the {@code "Connection"} header depending on what the default keep alive
     * mode of the message's protocol version is, as specified by
     * {@link HttpVersion#isKeepAliveDefault()}.
     * <ul>
     * <li>If the connection is kept alive by default:
     *     <ul>
     *     <li>set to {@code "close"} if {@code keepAlive} is {@code false}.</li>
     *     <li>remove otherwise.</li>
     *     </ul></li>
     * <li>If the connection is closed by default:
     *     <ul>
     *     <li>set to {@code "keep-alive"} if {@code keepAlive} is {@code true}.</li>
     *     <li>remove otherwise.</li>
     *     </ul></li>
     * </ul>
     */
    public static void setKeepAlive(HttpMessage message, boolean keepAlive)
    {
        if (message.getProtocolVersion().isKeepAliveDefault())
        {
            if (keepAlive)
            {
                message.removeHeader(HttpConstants.HEADER_CONNECTION);
            }
            else
            {
                message.setHeader(HttpConstants.HEADER_CONNECTION, HttpConstants.HEADER_CONNECTION_CLOSE);
            }
        }
        else
        {
            if (keepAlive)
            {
                message.setHeader(HttpConstants.HEADER_CONNECTION, HttpConstants.HEADER_CONNECTION_KEEP_ALIVE);
            }
            else
            {
                message.removeHeader(HttpConstants.HEADER_CONNECTION);
            }
        }
    }
}

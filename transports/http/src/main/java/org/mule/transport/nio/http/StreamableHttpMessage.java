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

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.nio.tcp.ChannelReceiverResource;

/**
 * Defines an {@link HttpMessage} can receive and provide a streamed content source.
 * Also acts as a {@link ChannelReceiverResource} when received in Mule.</br> This
 * interface can appear confusing because it supports a dual mode as HTTP
 * requests/responses are created when Mule receives or sends HTTP requests or
 * responses. See below for
 */
public interface StreamableHttpMessage extends HttpMessage, ChannelReceiverResource
{
    /**
     * This is used to perform the {@link Channel} - {@link StreamableHttpMessage}
     * association so HTTP chunks incoming on a particular {@link Channel} can be
     * routed to the right {@link StreamableHttpMessage} (which has already been
     * dispatched in Mule's infrastructure but still requires receiving data for
     * chunked HTTP requests or responses).
     */
    Channel getChannel();

    /**
     * Used when Mule:
     * <ul>
     * <li>receives an HTTP request (in {@link StreamableHttpRequest}),</li>
     * <li>receives the response from a remote HTTP request (in
     * {@link StreamableHttpResponse}).</li>
     * </ul>
     */
    Object getPayload();

    /**
     * Used when Mule:
     * <ul>
     * <li>sends an HTTP request (in {@link StreamableHttpRequest}),</li>
     * <li>sends an response to an inbound HTTP request (in
     * {@link StreamableHttpResponse}).</li>
     * </ul>
     */
    OutputHandler getStreamingContent();

    void setStreamingContent(final OutputHandler oh, final MuleEvent event) throws IOException;

    boolean hasStreamingContent();

    /**
     * Inform that the last chunk of a chunked message has been received.
     */
    void lastChunkReceived();
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.io;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * An {@link OutputStream} that can write to a {@link Channel}.
 */
public class ChannelOutputStream extends BufferedOutputStream
{
    protected static class RawChannelOutputStream extends OutputStream
    {
        private final Channel channel;
        private volatile ChannelFuture channelFuture;

        public RawChannelOutputStream(final Channel channel)
        {
            this.channel = channel;
        }

        @Override
        public void write(final int b) throws IOException
        {
            throw new UnsupportedOperationException(
                "wrap with a BufferedOutputStream should prevent this to be called!");
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException
        {
            final ChannelBuffer chunk = ChannelBuffers.copiedBuffer(b, off, len);
            channelFuture = channel.write(chunk);
        }
    }

    private final RawChannelOutputStream rcos;

    public ChannelOutputStream(final Channel channel)
    {
        this(new RawChannelOutputStream(channel));
    }

    private ChannelOutputStream(final ChannelOutputStream.RawChannelOutputStream rcos)
    {
        super(rcos);
        this.rcos = rcos;
    }

    public ChannelFuture getChannelFuture()
    {
        return rcos.channelFuture;
    }
}

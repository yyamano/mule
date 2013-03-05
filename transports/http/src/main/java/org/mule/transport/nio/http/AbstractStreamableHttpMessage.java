
package org.mule.transport.nio.http;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpMessage;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.transport.nio.tcp.protocols.StreamingProtocol;

/**
 * Provides common code for {@link StreamableHttpRequest} and
 * {@link StreamableHttpResponse}.
 */
public abstract class AbstractStreamableHttpMessage extends DefaultHttpMessage
    implements StreamableHttpMessage
{
    protected static final StreamingProtocol TCP_PROTOCOL = new StreamingProtocol();

    protected final Channel channel;
    protected final ChannelInputStream channelInputStream;
    protected volatile OutputHandler outputHandler;
    protected volatile boolean expectingChunks;

    protected AbstractStreamableHttpMessage(final HttpVersion version,
                                            final Collection<Entry<String, String>> headers,
                                            final ChannelBuffer content,
                                            final boolean chunked,
                                            final Channel channel)
    {
        super(version);
        this.channel = channel;
        channelInputStream = chunked ? new ChannelInputStream(channel, TCP_PROTOCOL) : null;

        setChunked(chunked);
        expectingChunks = chunked;
        setContent(content);

        if (headers != null)
        {
            for (final Entry<String, String> header : headers)
            {
                addHeader(header.getKey(), header.getValue());
            }
        }
    }

    protected void initiliase()
    {
        final long contentLength = HttpHeaders.getContentLength(this);
        if ((channelInputStream != null) && (contentLength > 0))
        {
            channelInputStream.setExpectedBytes(contentLength);
        }
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean isActive()
    {
        return expectingChunks && channelInputStream.isActive();
    }

    public void lastChunkReceived()
    {
        Validate.isTrue(isChunked(), "non chunked http message doesn't support lastChunkReceived");
        expectingChunks = false;
    }

    public Object getPayload()
    {
        if (channelInputStream != null)
        {
            return channelInputStream;
        }

        return getContent() == ChannelBuffers.EMPTY_BUFFER ? null : getContent().readBytes(
            getContent().readableBytes()).array();
    }

    public Channel getChannel()
    {
        return channel;
    }

    public ChannelInputStream getChannelInputStream()
    {
        return channelInputStream;
    }

    public boolean hasStreamingContent()
    {
        return outputHandler != null;
    }

    public void setStreamingContent(final OutputHandler outputHandler, final MuleEvent event)
        throws IOException
    {
        Validate.notNull(outputHandler, "outputHandler can't be null");

        if (HttpUtils.hasProtocolOlderThan(this, HttpVersion.HTTP_1_1))
        {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            outputHandler.write(event, baos);

            // HTTP/1.0 needs a content length -> drain stream
            setContent(ChannelBuffers.wrappedBuffer(baos.toByteArray()));
        }
        else
        {
            this.outputHandler = outputHandler;
            setChunked(true);
        }
    }

    public OutputHandler getStreamingContent()
    {
        if (outputHandler == null)
        {
            throw new IllegalStateException("Can't get a streaming content if outputHandler is null");
        }
        return outputHandler;
    }
}

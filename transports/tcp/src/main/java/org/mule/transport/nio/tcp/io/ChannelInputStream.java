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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.mule.api.MuleEvent;
import org.mule.transport.nio.tcp.ChannelReceiverResource;
import org.mule.transport.nio.tcp.TcpProtocol;
import org.mule.transport.nio.tcp.protocols.StreamingProtocol;
import org.mule.util.concurrent.Latch;

/**
 * An {@link InputStream} designed to stream data from {@link Channel}s and that acts
 * like a {@link PipedInputStream} but with a growable (but bounded) backing buffer.
 * It is able to auto-flush when no more data is expected, based either on a time-out
 * or a provided expected amount of bytes.<br/>
 * <br/>
 * This class is the heart of the NIO transport: its ultimate goal consists in
 * adapting flows of inbound byte arrays (the natural output of NIO) into
 * {@link InputStream}s (the natural input of Mule). Indeed an {@link InputStream}
 * can be dispatched wrapped in a single {@link MuleEvent}, while disjoint inbound
 * byte arrays, which all pertains to the same conceptual message, could not be
 * routed individually into Mule's downstream infrastructure. Hence the idea to
 * provide them through a single {@link InputStream} object that can be easily routed
 * once and only once.
 */
public class ChannelInputStream extends InputStream implements ChannelReceiverResource
{
    protected static final Log LOGGER = LogFactory.getLog(ChannelInputStream.class);
    protected static final long CHANNEL_BACK_PRESSURE_TIMEOUT_MILLIS = 30000L;
    // TODO make this configurable?
    protected static final long STREAMING_MAX_DATA_WAIT_MILLIS = 100L;
    protected static final int STREAMING_BUFFER_QUEUE_SIZE = 10;
    /**
     * Maximum size before we start blocking on offer() operations to
     * push-back/flow-control the inbound data channel.
     */
    protected static final int DEFAULT_MAXIMUM_AVAILABLE_SIZE = 1024 * 1024;

    /**
     * Used to mark the termination of data flow in the
     * <code>BlockingQueue&lt;ByteBuffer&gt;</code>.
     */
    protected static final ByteBuffer CHANNEL_CLOSED_POISON_PILL = ByteBuffer.allocate(0);

    protected final Channel channel;
    protected final AtomicInteger availableBytes;
    protected final AtomicLong totalBytesReceived;
    protected final AtomicLong expectedBytes;
    protected final BlockingQueue<ByteBuffer> byteBuffers;

    protected volatile long maxDataAvailable = DEFAULT_MAXIMUM_AVAILABLE_SIZE;
    protected volatile long maxDataWait = STREAMING_MAX_DATA_WAIT_MILLIS;
    protected volatile boolean open;
    protected volatile Latch flowControlLatch;
    protected volatile boolean expectingBytes;
    protected volatile ByteBuffer currentByteBuffer;
    protected volatile Runnable beforeCloseAction;
    protected volatile Runnable afterCloseAction;
    protected volatile boolean deactivate;

    public ChannelInputStream(final Channel channel, final TcpProtocol protocol)
    {
        Validate.notNull(channel, "channel can't be null");
        this.channel = channel;

        maxDataWait = protocol instanceof StreamingProtocol ? STREAMING_MAX_DATA_WAIT_MILLIS : Long.MAX_VALUE;

        availableBytes = new AtomicInteger(0);
        expectedBytes = new AtomicLong(0);
        totalBytesReceived = new AtomicLong(0);
        byteBuffers = new LinkedBlockingQueue<ByteBuffer>();

        open = true;
        currentByteBuffer = null;
        expectingBytes = false;
        deactivate = false;

        channel.getCloseFuture().addListener(new ChannelFutureListener()
        {
            public void operationComplete(final ChannelFuture future) throws Exception
            {
                byteBuffers.offer(CHANNEL_CLOSED_POISON_PILL);
            }
        });
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public boolean isActive()
    {
        return !deactivate && (channel.isOpen() || (!channel.isOpen() && isOpen() && availableBytes.get() > 0));
    }

    public void offer(final byte[] bytes) throws IOException
    {
        Validate.notNull(bytes, "bytes can't be null");

        if (bytes.length == 0)
        {
            return;
        }

        if (!isOpen())
        {
            throw new EOFException("Attempt to write to a closed stream, discarding bytes: "
                                   + Arrays.toString(bytes));
        }

        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);

        if (available() >= maxDataAvailable)
        {
            ponderForSpace();
        }

        // block if the buffer is full in order to apply back pressure on the
        // channel that is offering data
        byteBuffers.offer(byteBuffer);

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("Enqueued: %d bytes in: %s", bytes.length, this));
        }

        availableBytes.addAndGet(bytes.length);
        totalBytesReceived.addAndGet(bytes.length);
    }

    protected void ponderForSpace() throws IOException, InterruptedIOException
    {
        final long startTime = System.currentTimeMillis();

        while ((available() >= maxDataAvailable)
               && (System.currentTimeMillis() - startTime < CHANNEL_BACK_PRESSURE_TIMEOUT_MILLIS))
        {
            flowControlLatch = new Latch();
            try
            {
                @SuppressWarnings("unused")
                final boolean released = flowControlLatch.await(CHANNEL_BACK_PRESSURE_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS);
            }
            catch (final InterruptedException ie)
            {
                throw new InterruptedIOException("Failed to enqueue bytes (interrupted)");
            }
        }

        if (available() >= maxDataAvailable)
        {
            throw new InterruptedIOException("Failed to enqueue bytes (buffer over limit of: "
                                             + maxDataAvailable + ")");
        }
    }

    @Override
    public void close() throws IOException
    {
        if (!isOpen())
        {
            return;
        }

        if (beforeCloseAction != null)
        {
            beforeCloseAction.run();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format("Before close action: %s run on: %s", beforeCloseAction, this));
            }
        }

        super.close();
        open = false;

        if (afterCloseAction != null)
        {
            afterCloseAction.run();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format("After close action: %s run on: %s", afterCloseAction, this));
            }
        }
    }

    @Override
    public int read(final byte[] b) throws IOException
    {
        final ByteBuffer dataToRead = getDataToRead();

        if (dataToRead != null)
        {
            final int bytesToRead = Math.min(dataToRead.remaining(), b.length);
            dataToRead.get(b, 0, bytesToRead);
            decrementAvailable(bytesToRead);
            return bytesToRead;
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException
    {
        final ByteBuffer dataToRead = getDataToRead();

        if (dataToRead != null)
        {
            final int bytesToRead = Math.min(dataToRead.remaining(), len);
            dataToRead.get(b, off, bytesToRead);
            decrementAvailable(bytesToRead);
            return bytesToRead;
        }
        else
        {
            return -1;
        }
    }

    @Override
    public int read() throws IOException
    {
        final ByteBuffer dataToRead = getDataToRead();

        if (dataToRead != null)
        {
            decrementAvailable(1);
            return dataToRead.get() & 0xFF;
        }
        else
        {
            return -1;
        }
    }

    protected void decrementAvailable(final int delta)
    {
        availableBytes.addAndGet(-delta);
        if (flowControlLatch != null)
        {
            flowControlLatch.release();
        }
    }

    protected ByteBuffer getDataToRead() throws IOException
    {
        if (!isOpen())
        {
            throw new EOFException("Reading from closed input stream: " + this);
        }

        if ((currentByteBuffer != null) && (currentByteBuffer.hasRemaining()))
        {
            return currentByteBuffer;
        }

        if ((byteBuffers.isEmpty()) && (!channel.isOpen()))
        {
            return null;
        }

        final long currentExpectedBytes = expectedBytes.get();
        if ((expectingBytes) && (available() == 0) && (currentExpectedBytes > 0)
            && (totalBytesReceived.get() >= currentExpectedBytes))
        {
            expectingBytes = false;

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format(
                    "Not waiting for further data: received %d bytes of %d expected (and %d byte(s) available left)",
                    totalBytesReceived.get(), currentExpectedBytes, available()));
            }

            return null;
        }

        try
        {
            final ByteBuffer polledByteBuffer = byteBuffers.poll(maxDataWait, TimeUnit.MILLISECONDS);

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format("Polled %s with %d buffer(s) in queue and %d byte(s) available",
                    polledByteBuffer, byteBuffers.size(), available()));
            }

            currentByteBuffer = polledByteBuffer == CHANNEL_CLOSED_POISON_PILL ? null : polledByteBuffer;
        }
        catch (final InterruptedException ie)
        {
            // Restore the interrupted status
            Thread.currentThread().interrupt();
            return null;
        }

        return currentByteBuffer;
    }

    @Override
    public int available()
    {
        return availableBytes.get();
    }

    public boolean isOpen()
    {
        return open;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public long getMaxDataWait()
    {
        return maxDataWait;
    }

    // TODO make this configurable on the connector
    public void setMaxDataWait(final long maxDataWait)
    {
        this.maxDataWait = maxDataWait;
    }

    public long getMaxDataAvailable()
    {
        return maxDataAvailable;
    }

    // TODO make this configurable on the connector
    public void setMaxDataAvailable(final long maxDataAvailable)
    {
        this.maxDataAvailable = maxDataAvailable;
    }

    public boolean hasBeforeCloseAction()
    {
        return beforeCloseAction != null;
    }

    public void setBeforeCloseAction(final Runnable closeAction)
    {
        Validate.isTrue(beforeCloseAction == null, "can't re-assign beforeCloseAction");
        beforeCloseAction = closeAction;
    }

    public boolean hasAfterCloseAction()
    {
        return afterCloseAction != null;
    }

    public void setAfterCloseAction(final Runnable closeAction)
    {
        Validate.isTrue(afterCloseAction == null, "can't re-assign afterCloseAction");
        afterCloseAction = closeAction;
    }

    /**
     * Tell the {@link ChannelInputStream} that we expect it to <b>at least</b> be
     * receiving the specified amount of bytes. This influences its internal
     * algorithm for deciding if it is worth waiting for more data to come or not.
     * 
     * @param expectedBytes
     */
    public void setExpectedBytes(final long expectedBytes)
    {
        Validate.isTrue(expectedBytes > 0, "expectedBytes must be > 0");

        this.expectedBytes.addAndGet(expectedBytes);
        expectingBytes = true;

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("Expected bytes set to: %d (currently received: %d)",
                this.expectedBytes.get(), totalBytesReceived.get()));
        }
    }

    /**
     * Tell the {@link ChannelInputStream} that we are unsure if extra bytes will
     * come or not. This influences its internal algorithm for deciding if it is
     * worth waiting for more data to come or not.
     */
    public void resetExpectedBytes()
    {
        expectingBytes = false;

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug(String.format("Reset expected bytes (currently received: %d)",
                totalBytesReceived.get()));
        }
    }
    
    public void deactivate()
    {
        deactivate = true;
    }
}

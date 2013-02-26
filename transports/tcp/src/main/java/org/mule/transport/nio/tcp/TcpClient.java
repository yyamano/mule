/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.ConnectorException;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.api.transport.ReceiveException;
import org.mule.transport.ConnectException;
import org.mule.transport.nio.tcp.i18n.TcpMessages;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.util.ExceptionUtils;
import org.mule.util.concurrent.Latch;

/**
 * A Netty powered TCP client used by {@link TcpMessageDispatcher},
 * {@link TcpMessageRequester} and {@link PollingTcpMessageReceiver} to send and
 * receive messages over TCP {@link Channel}s.
 */
public class TcpClient
{
    public static class TcpClientUpstreamHandler extends SimpleChannelUpstreamHandler
    {
        private final static Log LOGGER = LogFactory.getLog(TcpClientUpstreamHandler.class);

        protected final TcpClient tcpClient;

        public TcpClientUpstreamHandler(final TcpClient tcpClient)
        {
            this.tcpClient = tcpClient;
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent me) throws Exception
        {
            final Object message = me.getMessage();
            final Channel channel = me.getChannel();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Received message: " + message + " from channel: " + channel);
            }

            tcpClient.handleChannelData(channel, message);
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent ee)
            throws Exception
        {
            final Channel channel = ee.getChannel();

            final SocketAddress remoteAddress = channel.getRemoteAddress();
            final Throwable cause = ee.getCause();
            final Throwable rootCause = ExceptionUtils.getRootCause(cause);

            final String source = String.format("%s (remote address: %s)",
                tcpClient.connectable.getConnectionDescription(), remoteAddress);
            final Throwable sourceCause = rootCause == null ? cause : rootCause;
            final Exception lastException = sourceCause instanceof Exception
                                                                            ? (Exception) sourceCause
                                                                            : new EndpointException(
                                                                                TcpMessages.errorWhileHandlingResponseFrom(source),
                                                                                sourceCause);

            tcpClient.handleChannelException(channel, lastException);

            channel.close();
        }
    }

    protected static final long ONE_SECOND_MILLIS = 1000L;

    protected final Log logger = LogFactory.getLog(getClass());
    protected final TcpConnector tcpConnector;
    protected final Connectable connectable;
    protected final ImmutableEndpoint endpoint;
    protected final NioClientSocketChannelFactory clientSocketChannelFactory;
    protected final MuleMessageFactory muleMessageFactory;

    protected volatile Latch messageDeliveredLatch;
    protected volatile ChannelReceiverResource activeChannelReceiverResource;
    protected volatile Exception lastException;

    protected Channel channel;
    protected ClientBootstrap clientBootstrap;

    public TcpClient(final TcpConnector tcpConnector,
                     final Connectable connectable,
                     final ImmutableEndpoint endpoint) throws CreateException
    {
        Validate.notNull(tcpConnector, "tcpConnector can't be null");
        Validate.notNull(connectable, "connectable can't be null");
        Validate.notNull(endpoint, "endpoint can't be null");

        this.tcpConnector = tcpConnector;
        this.endpoint = endpoint;
        this.connectable = connectable;

        if (endpoint instanceof InboundEndpoint)
        {
            clientSocketChannelFactory = tcpConnector.getRequesterClientSocketChannelFactory();
        }
        else if (endpoint instanceof OutboundEndpoint)
        {
            clientSocketChannelFactory = tcpConnector.getDispatcherClientSocketChannelFactory();
        }
        else
        {
            throw new IllegalArgumentException("Unsupported endpoint type: " + endpoint.getClass().getName());
        }

        muleMessageFactory = tcpConnector.getMuleMessageFactory();

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("%s initialized with: %s, %s, %s", this, tcpConnector, connectable,
                endpoint));;
        }
    }

    public boolean isValid()
    {
        return channel != null && channel.isConnected();
    }

    public boolean isKeepOpen()
    {
        return tcpConnector.isKeepSendSocketOpen();
    }

    public Connectable getConnectable()
    {
        return connectable;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public void connect() throws Exception
    {
        final URI uri = endpoint.getEndpointURI().getUri();

        try
        {
            clientBootstrap = new ClientBootstrap(clientSocketChannelFactory);

            clientBootstrap.setOption("reuseAddress", tcpConnector.isReuseAddress());
            clientBootstrap.setOption("tcpNoDelay", tcpConnector.isSendTcpNoDelay());
            clientBootstrap.setOption("keepAlive", tcpConnector.isKeepAlive());

            if (tcpConnector.getSocketMaxWait() != TcpConnector.INT_VALUE_NOT_SET)
            {
                clientBootstrap.setOption("connectTimeoutMillis", tcpConnector.getSocketMaxWait());
            }
            if (tcpConnector.getReceiveBufferSize() != TcpConnector.INT_VALUE_NOT_SET)
            {
                clientBootstrap.setOption("receiveBufferSize", tcpConnector.getReceiveBufferSize());
            }
            if (tcpConnector.getSendBufferSize() != TcpConnector.INT_VALUE_NOT_SET)
            {
                clientBootstrap.setOption("sendBufferSize", tcpConnector.getSendBufferSize());
            }
            if (tcpConnector.getSocketSoLinger() != TcpConnector.INT_VALUE_NOT_SET)
            {
                clientBootstrap.setOption("soLinger", tcpConnector.getSocketSoLinger());
            }

            clientBootstrap.setPipelineFactory(getPipelineFactory());

            setupDeliveryMechanism();
        }
        catch (final Exception e)
        {
            throw new DefaultMuleException(TcpMessages.failedToBindToUri(uri), ExceptionUtils.getRootCause(e));
        }

        channel = connectChannel();
        if (!channel.isConnected())
        {
            throw new ConnectException(TcpMessages.connectAttemptTimedOut(), connectable);
        }
    }

    protected void setupDeliveryMechanism()
    {
        lastException = null;
        messageDeliveredLatch = new Latch();
        activeChannelReceiverResource = null;
    }

    protected ChannelPipelineFactory getPipelineFactory()
    {
        return new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline() throws Exception
            {
                final ChannelPipeline p = Channels.pipeline();
                p.addLast("mule-tcp-client-handler", new TcpClientUpstreamHandler(TcpClient.this));
                return p;
            }
        };
    }

    public void disconnect() throws Exception
    {
        if ((channel != null) && (channel.isOpen()))
        {
            channel.close().awaitUninterruptibly();

            if (logger.isDebugEnabled())
            {
                logger.debug("Closed channel: " + channel);
            }
        }
    }

    public final void dispatch(final MuleEvent event) throws Exception
    {
        try
        {
            setUp(event);
            dispatchAndWaitUntilDispatched(event);
        }
        finally
        {
            returnToConnectorAndCleanup();
        }
    }

    public final MuleMessage send(final MuleEvent event) throws Exception
    {
        MuleMessage retrievedMessage = null;

        try
        {
            setUp(event);
            dispatchAndWaitUntilDispatched(event);
            retrievedMessage = retrieveRemoteResponse(event);
            return retrievedMessage;
        }
        finally
        {
            returnToConnectorAndCleanup(retrievedMessage);
        }
    }

    public final MuleMessage request(final long timeout) throws Exception
    {
        MuleMessage retrievedMessage = null;

        try
        {
            setUp(null);
            retrievedMessage = retrieveRemoteResponse(timeout);
            return retrievedMessage;
        }
        finally
        {
            returnToConnectorAndCleanup(retrievedMessage);
        }
    }

    protected void setUp(final MuleEvent event) throws Exception
    {
        // NOOP
    }

    protected void returnToConnectorAndCleanup()
    {
        returnToConnectorAndCleanup(null);
    }

    protected void returnToConnectorAndCleanup(final MuleMessage retrievedMessage)
    {
        try
        {
            if (retrievedMessage == null)
            {
                returnToConnectorNow();
            }
            else
            {
                returnToConnectorWhenPossible(retrievedMessage);
            }
        }
        catch (final Exception e)
        {
            logger.error("Failed to return: " + this + " to connector: " + tcpConnector, e);
        }
        finally
        {
            cleanUp();
        }
    }

    protected void cleanUp()
    {
        // NOOP
    }

    protected void returnToConnectorWhenPossible(final MuleMessage retrievedMessage) throws Exception
    {
        final Object payload = retrievedMessage.getPayload();
        if (payload instanceof ChannelInputStream)
        {
            // hook the return operation to the input stream
            ((ChannelInputStream) payload).setAfterCloseAction(new Runnable()
            {
                public void run()
                {
                    try
                    {
                        tcpConnector.returnTcpClient(TcpClient.this);
                    }
                    catch (final Exception e)
                    {
                        logger.error("Failed to return " + this + " to " + tcpConnector, e);
                    }
                }
            });

            if (logger.isDebugEnabled())
            {
                logger.debug(this + " differed return to connector until closing of: " + payload);
            }
        }
        else
        {
            returnToConnectorNow();
        }
    }

    protected void returnToConnectorNow() throws Exception
    {
        tcpConnector.returnTcpClient(this);
    }

    protected final void dispatchAndWaitUntilDispatched(final MuleEvent event) throws Exception
    {
        final ChannelFuture channelFuture = doDispatch(event);

        // block until the dispatch is fully done, avoiding await() to prevent
        // permanent blocking when something goes haywire
        while (!channelFuture.isDone())
        {
            channelFuture.await(getTimeout(event));
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(this + " has dispatched: [" + event + "] on channel: " + channel);
        }
    }

    protected int getTimeout(final MuleEvent event)
    {
        final int endpointResponseTimeout = endpoint.getResponseTimeout();

        if (endpointResponseTimeout != event.getMuleContext().getConfiguration().getDefaultResponseTimeout())
        {
            return endpointResponseTimeout;
        }

        return event.getTimeout();
    }

    protected ChannelFuture doDispatch(final MuleEvent event) throws Exception
    {
        return tcpConnector.write(event.getMessage(), channel);
    }

    protected Channel connectChannel() throws Exception
    {
        final ChannelFuture channelFuture = clientBootstrap.connect(getRemoteSocketAddress());

        channelFuture.await(endpoint.getResponseTimeout());

        if (!channelFuture.isSuccess())
        {
            if (lastException != null)
            {
                throw lastException;
            }

            throw new ConnectorException(TcpMessages.failedToConnectChannelForEndpoint(endpoint),
                tcpConnector, channelFuture.getCause());
        }

        return channelFuture.getChannel();
    }

    protected InetSocketAddress getRemoteSocketAddress()
    {
        final EndpointURI uri = endpoint.getEndpointURI();
        return new InetSocketAddress(uri.getHost(), uri.getPort());
    }

    protected MuleMessage retrieveRemoteResponse(final long timeout) throws Exception
    {
        return retrieveRemoteResponse(null, timeout);
    }

    protected MuleMessage retrieveRemoteResponse(final MuleEvent event) throws Exception
    {
        return retrieveRemoteResponse(event, endpoint.getResponseTimeout());
    }

    protected final MuleMessage retrieveRemoteResponse(final MuleEvent event, final long timeout)
        throws Exception
    {
        final Object response = waitUntilResponseDelivered(timeout);
        final Object transportMessage = buildResponseTransportMessage(response);
        return muleMessageFactory.create(transportMessage, endpoint.getEncoding());
    }

    protected Object waitUntilResponseDelivered(final long timeout) throws Exception
    {
        if (activeChannelReceiverResource != null)
        {
            return activeChannelReceiverResource;
        }

        // fake "forever" with Long.MAX_VALUE
        final long actualTimeOut = timeout == -1 ? Long.MAX_VALUE : timeout;

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for response message for a maximum of: " + actualTimeOut + "ms");
        }

        final boolean messageReceived = messageDeliveredLatch.await(actualTimeOut, TimeUnit.MILLISECONDS);

        if ((!messageReceived) && (lastException != null))
        {
            throw lastException;
        }

        if (activeChannelReceiverResource == null)
        {
            throw new ReceiveException(endpoint, timeout, new TimeoutException());
        }

        return activeChannelReceiverResource;
    }

    protected Object buildResponseTransportMessage(final Object response) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Building transport message from: " + response);
        }
        final ChannelInputStream cis = (ChannelInputStream) response;
        final Object decodedResponse = tcpConnector.getTcpProtocol().read(cis);
        cis.resetExpectedBytes();
        return decodedResponse;
    }

    protected void handleChannelException(final Channel channel, final Exception e)
    {
        lastException = e;
    }

    protected void handleChannelData(final Channel channel, final Object message) throws Exception
    {
        final ChannelBuffer channelBuffer = (ChannelBuffer) message;
        final int readableBytes = channelBuffer.readableBytes();

        if (readableBytes == 0)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Ignoring 0 bytes received from channel: " + channel);
            }

            return;
        }

        final byte[] data = channelBuffer.readBytes(readableBytes).array();
        final ChannelInputStream cis = getChannelReceiverResource(channel, new Callable<ChannelInputStream>()
        {
            public ChannelInputStream call() throws Exception
            {
                return new ChannelInputStream(channel, tcpConnector.getTcpProtocol());
            }
        });

        cis.offer(data);
        messageDeliveredLatch.countDown();
        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Delivered: %s received from channel: %s", cis, channel));
        }
    }

    @SuppressWarnings("unchecked")
    protected <T extends ChannelReceiverResource> T getChannelReceiverResource(final Channel channel,
                                                                               final Callable<T> resourceCreator)
        throws Exception
    {
        if (activeChannelReceiverResource != null)
        {
            return (T) activeChannelReceiverResource;
        }

        activeChannelReceiverResource = resourceCreator.call();
        return (T) activeChannelReceiverResource;
    }
}

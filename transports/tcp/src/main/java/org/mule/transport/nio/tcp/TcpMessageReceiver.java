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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.ConnectorException;
import org.mule.config.MutableThreadingProfile;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.AbstractReceiverResourceWorker;
import org.mule.transport.AbstractTransportMessageProcessTemplate;
import org.mule.transport.ConnectException;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.tcp.i18n.TcpMessages;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.transport.nio.tcp.notifications.TcpSocketNotification;
import org.mule.transport.nio.tcp.protocols.StreamingProtocol;
import org.mule.util.ExceptionUtils;
import org.mule.util.StringUtils;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.monitor.Expirable;

import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.util.internal.ConcurrentHashMap;

/**
 * <code>TcpMessageReceiver</code> acts like a server to receive incoming requests.
 */
public class TcpMessageReceiver extends AbstractMessageReceiver
{
    protected static class TcpMessageReceiverUpstreamHandler extends SimpleChannelUpstreamHandler
    {
        private final TcpMessageReceiver receiver;

        public TcpMessageReceiverUpstreamHandler(final TcpMessageReceiver receiver)
        {
            super();
            this.receiver = receiver;
        }

        @Override
        public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e)
            throws Exception
        {
            receiver.registerChannel(e.getChannel());
            receiver.fireNotification(e.getChannel(), TcpSocketNotification.CONNECTION);
            super.channelConnected(ctx, e);

            sendServerGreeting(e);
        }

        @Override
        public void channelDisconnected(
                ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            receiver.fireNotification(e.getChannel(), TcpSocketNotification.DISCONNECTION);
            super.channelDisconnected(ctx, e);
        }

            protected void sendServerGreeting(final ChannelStateEvent e) throws UnsupportedEncodingException
        {
            final String serverGreeting = receiver.getTcpConnector().getServerGreeting();
            if (StringUtils.isNotBlank(serverGreeting))
            {
                final MuleContext muleContext = receiver.getConnector().getMuleContext();
                final ExpressionManager expressionManager = muleContext.getExpressionManager();

                final boolean isExpression = expressionManager.isExpression(serverGreeting);

                final MuleMessage emptyMuleMessage = new DefaultMuleMessage(NullPayload.getInstance(),
                    muleContext);
                final MuleEvent emptyMuleEvent = new DefaultMuleEvent(emptyMuleMessage,
                    receiver.getEndpoint(), (FlowConstruct) null);

                final String actualServerGreeting = isExpression ? expressionManager.evaluate(serverGreeting,
                    emptyMuleEvent, true).toString() : serverGreeting;

                final byte[] greetingBytes = actualServerGreeting.getBytes(muleContext.getConfiguration()
                    .getDefaultEncoding());
                e.getChannel().write(ChannelBuffers.wrappedBuffer(greetingBytes));
            }
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent event)
        {
            final Channel channel = event.getChannel();
            try
            {
                receiver.handleChannelData(channel, event.getMessage());
            }
            catch (final Exception e)
            {
                receiver.handleException(e);
                channel.close();
            }
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e)
        {
            if ((receiver.isStopping()) || (!receiver.isStarted()) || (!receiver.getConnector().isStarted()))
            {
                return;
            }

            if (e.getCause() instanceof Exception)
            {
                receiver.handleException((Exception) e.getCause());
            }
            else
            {
                receiver.handleException(new ConnectorException(
                    TcpMessages.errorWhileHandlingRequestInReceiver(receiver), receiver.getConnector(),
                    e.getCause()));
            }
        }
    }

    /**
     * Takes care of scheduling {@link TcpMessageReceiverRouterWorker} instead of
     * letting Netty's threads do it. Indeed, using Netty's threads can lead to a
     * complete lock-down of the receiving infrastructure because
     * {@link TcpMessageReceiverRouterWorker}'s threads can be blocked waiting for
     * more data to arrive while Netty's threads are blocked trying to schedule more
     * {@link TcpMessageReceiverRouterWorker}s. Also culls expired
     * {@link ChannelReceiverResource}s.
     */
    protected static class ChannelReceiverResourceManager implements Work
    {
        private static final Log LOGGER = LogFactory.getLog(ChannelReceiverResourceManager.class);

        protected final TcpMessageReceiver tcpReceiver;
        protected ExecutorService requestHandOffExecutor;

        public ChannelReceiverResourceManager(final TcpMessageReceiver tcpReceiver)
        {
            this.tcpReceiver = tcpReceiver;
            this.requestHandOffExecutor = createRequestDispatcherThreadPool(tcpReceiver.tcpConnector);
        }

        private ExecutorService createRequestDispatcherThreadPool(TcpConnector tcpConnector)
        {
            ThreadingProfile receiverThreadingProfile = tcpConnector.getReceiverThreadingProfile();
            MutableThreadingProfile dispatcherThreadingProfile = new MutableThreadingProfile(receiverThreadingProfile);
            dispatcherThreadingProfile.setThreadFactory(null);
            dispatcherThreadingProfile.setMaxThreadsActive(dispatcherThreadingProfile.getMaxThreadsActive()*2);
            ExecutorService executorService = dispatcherThreadingProfile.createPool("tcp-request-dispatch-" + tcpReceiver.getReceiverKey());
            return executorService;
        }

        public void run()
        {
            while (!tcpReceiver.disposing.get())
            {
                try
                {
                    dispatchPendingChannelReceiverResources();
                }
                catch (final Exception e)
                {
                    tcpReceiver.handleException(e);
                }

                try
                {
                    cullInactiveChannelReceiverResources();
                }
                catch (final Exception e)
                {
                    tcpReceiver.handleException(e);
                }
            }
        }

        protected void dispatchPendingChannelReceiverResources() throws InterruptedException, WorkException
        {
            ChannelReceiverResource crr = null;

            while ((crr = tcpReceiver.channelReceiverResourcePendingWorkerAssignment.poll(1, TimeUnit.SECONDS)) != null)
            {
                // This was the original way of executing work. This changed to support throttling.
//                final Work worker = tcpReceiver.newRouterWorker(crr);
//                tcpReceiver.getWorkManager().scheduleWork(worker, WorkManager.INDEFINITE, null,
//                    tcpReceiver.getTcpConnector());
                
                Work worker = tcpReceiver.newDispatcherWorker(crr);
                // Process each channel in a different thread so we can continue processing other channels right away.
                requestHandOffExecutor.execute(worker);

                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("Scheduled work: " + worker);
                }
            }
        }

        protected void cullInactiveChannelReceiverResources()
        {
            for (final Iterator<Entry<Channel, ChannelReceiverResource>> i = tcpReceiver.activeChannelReceiverResource.entrySet()
                .iterator(); i.hasNext();)
            {
                final ChannelReceiverResource crr = i.next().getValue();

                if (!crr.isActive())
                {
                    i.remove();

                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Culled: " + crr);
                    }
                }
            }
        }

        public void release()
        {
            requestHandOffExecutor.shutdown();
        }
    }

    protected static class TcpMessageReceiverRouterWorker extends AbstractReceiverResourceWorker
        implements Disposable, Expirable
    {
        private static final Log LOGGER = LogFactory.getLog(TcpMessageReceiverRouterWorker.class);

        protected final TcpMessageReceiver tcpReceiver;
        protected final TcpProtocol protocol;
        protected final ChannelInputStream dataIn;
        protected final Object notify = new Object();

        protected volatile boolean running = true;
        protected volatile boolean moreMessages = true;

        public TcpMessageReceiverRouterWorker(final ChannelInputStream channelInputStream,
                                              final TcpMessageReceiver tcpReceiver)
        {
            super(channelInputStream.getChannel(), tcpReceiver,
                TcpConnector.getOutputStream(channelInputStream.getChannel()));
            this.tcpReceiver = tcpReceiver;
            protocol = tcpReceiver.tcpConnector.getTcpProtocol();
            dataIn = channelInputStream;

            dataIn.setBeforeCloseAction(new Runnable()
            {
                public void run()
                {
                    // Don't actually close the stream, we just want to know if the
                    // we want to stop receiving messages on this channel.
                    // The Protocol is responsible for closing this.
                    moreMessages = false;
                }
            });
        }

        @Override
        public void release()
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Releasing: " + this);
            }

            super.release();
        }

        @Override
        public void dispose()
        {
            running = false;
        }

        @Override
        public void expired()
        {
            running = false;
        }

        @Override
        protected void bindTransaction(final Transaction tx) throws TransactionException
        {
            // nothing to do
        }

        @Override
        protected Object getNextMessage(final Object resource) throws Exception
        {
            final long keepAliveTimeout = tcpReceiver.tcpConnector.getKeepAliveTimeout();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Getting next message with keepAliveTimeout=" + keepAliveTimeout);
            }

            Object readMsg = null;
            try
            {
                // Create a monitor if expiry was set
                if (keepAliveTimeout > 0)
                {
                    tcpReceiver.tcpConnector.getKeepAliveMonitor().addExpirable(keepAliveTimeout,
                        TimeUnit.MILLISECONDS, this);
                }

                if (dataIn.isOpen())
                {
                    readMsg = protocol.read(dataIn);
                }

                // There was some action so we can clear the monitor
                tcpReceiver.tcpConnector.getKeepAliveMonitor().removeExpirable(this);

                if (protocol instanceof StreamingProtocol)
                {
                    // downstream will deal with the inputstream as a single message
                    moreMessages = false;
                }
                else
                {
                    // get ready for potentially more data
                    dataIn.resetExpectedBytes();
                }

                return readMsg;
            }
            catch (final InterruptedIOException e)
            {
                tcpReceiver.tcpConnector.getKeepAliveMonitor().removeExpirable(this);
            }

            return null;
        }

        @Override
        protected boolean hasMoreMessages(final Object message)
        {
            final boolean result = running && moreMessages && dataIn.isOpen() && !tcpReceiver.disposing.get();

            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(String.format(
                    "hasMoreMessages=%s [running=%s, moreMessages=%s, dataIn.isOpen()=%s, tcpReceiver.disposing.get()=%s]",
                    result, running, moreMessages, dataIn.isOpen(), !tcpReceiver.disposing.get()));
            }

            return result;
        }

        @Override
        public void processMessages() throws Exception
        {
            if (tcpReceiver.disposing.get())
            {
                return;
            }
            super.processMessages();
        }

        @Override
        protected void handleResults(@SuppressWarnings("rawtypes") final List messages) throws Exception
        {
            // should send back only if remote synch is set or no outbound endpoints
            if (endpoint.getExchangePattern().hasResponse())
            {
                for (final Iterator<?> iterator = messages.iterator(); iterator.hasNext();)
                {
                    final Object o = iterator.next();
                    final Channel channel = (Channel) resource;
                    if (!channel.isOpen())
                    {
                        LOGGER.warn("Discarded response " + o
                                    + " that can't be delivered to closed channel: " + channel);
                    }
                    final ChannelFuture channelFuture = tcpReceiver.tcpConnector.write(o, channel);
                    channelFuture.await();
                }
            }
        }
    }

    protected final TcpConnector tcpConnector;
    protected final AtomicBoolean disposing;

    protected final ConcurrentMap<Channel, ChannelReceiverResource> activeChannelReceiverResource;
    protected final BlockingQueue<ChannelReceiverResource> channelReceiverResourcePendingWorkerAssignment;

    protected ServerBootstrap serverBootstrap;
    protected ChannelGroup receiverChannels;
    
    private ExecutorService workerExecutor;
    private ChannelReceiverResourceManager channelReceiverResourceManager;

    public TcpMessageReceiver(final Connector connector,
                              final FlowConstruct flowConstruct,
                              final InboundEndpoint endpoint) throws CreateException
    {
        super(connector, flowConstruct, endpoint);
        tcpConnector = (TcpConnector) connector;
        disposing = new AtomicBoolean(false);
        activeChannelReceiverResource = new ConcurrentHashMap<Channel, ChannelReceiverResource>();
        channelReceiverResourcePendingWorkerAssignment = new LinkedBlockingQueue<ChannelReceiverResource>();
        System.out.println("NIO TcpMessageReceiver");
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        receiverChannels = new DefaultChannelGroup(this.getReceiverKey() + ".receiver-channels");
        final MuleContext muleContext = connector.getMuleContext();

        final NamedThreadFactory threadFactory = new NamedThreadFactory(String.format("%s[%s].receiver",
            ThreadNameHelper.receiver(muleContext, connector.getName()), getReceiverKey()),
            muleContext.getExecutionClassLoader());

        final ThreadingProfile receiverTp = connector.getReceiverThreadingProfile();

        workerExecutor = new ThreadPoolExecutor(receiverTp.getMaxThreadsActive(),
            receiverTp.getMaxThreadsActive(), receiverTp.getThreadTTL(), TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<Runnable>(1000), threadFactory, new ThreadPoolExecutor.AbortPolicy());

        serverBootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
            tcpConnector.getBossExecutor(), workerExecutor));

        serverBootstrap.setOption("reuseAddress", tcpConnector.isReuseAddress());
        serverBootstrap.setOption("keepAlive", tcpConnector.isKeepAlive());

        if (tcpConnector.getSocketMaxWait() != TcpConnector.INT_VALUE_NOT_SET)
        {
            serverBootstrap.setOption("connectTimeoutMillis", tcpConnector.getSocketMaxWait());
        }
        if (tcpConnector.getReceiveBacklog() != TcpConnector.INT_VALUE_NOT_SET)
        {
            serverBootstrap.setOption("backlog", tcpConnector.getReceiveBacklog());
        }
        if (tcpConnector.getReceiveBufferSize() != TcpConnector.INT_VALUE_NOT_SET)
        {
            serverBootstrap.setOption("receiveBufferSize", tcpConnector.getReceiveBufferSize());
        }

        serverBootstrap.setPipelineFactory(getPipelineFactory());

        channelReceiverResourceManager = new ChannelReceiverResourceManager(this);
        try
        {
            getWorkManager().startWork(channelReceiverResourceManager, WorkManager.INDEFINITE,
                null, getTcpConnector());
        }
        catch (final WorkException we)
        {
            throw new InitialisationException(we, this);
        }
    }

    @Override
    public void doConnect() throws ConnectException
    {
        disposing.set(false);
        URI uri = null;

        try
        {
            uri = endpoint.getEndpointURI().getUri();

            final String host = StringUtils.defaultIfEmpty(uri.getHost(), "localhost");

            InetSocketAddress inetSocketAddress = null;
            if (host.trim().equals("localhost")) {
                inetSocketAddress = new InetSocketAddress(uri.getPort());
            } else {
                inetSocketAddress = new InetSocketAddress(host, uri.getPort());
            }

            final Channel channel = serverBootstrap.bind(inetSocketAddress);
            tcpConnector.registerReceiverChannel(channel);
            receiverChannels.add(channel);
        }
        catch (final Exception e)
        {
            final Throwable rootCause = ExceptionUtils.getRootCause(e);
            throw new ConnectException(TcpMessages.failedToBindToUri(uri), rootCause == null ? e : rootCause,
                this);
        }
    }

    @Override
    public void doDisconnect() throws ConnectException
    {
        disposing.set(true);
        receiverChannels.close().awaitUninterruptibly();
    }
    
    @Override
    protected void doDispose()
    {
        channelReceiverResourceManager.release();
        // It's tempting to call serverBootstrap.releaseExternalResources(); but the bossExecutor is 
        // shared among various receivers and has a different lifecycle since the connector owns it.
        // So we just shutdown the executor we've created here.
        workerExecutor.shutdown();
        super.doDispose();
    }

    protected ChannelPipelineFactory getPipelineFactory()
    {
        return new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline() throws Exception
            {
                final ChannelPipeline p = Channels.pipeline();
                p.addLast("mule-tcp-message-receiver-handler", new TcpMessageReceiverUpstreamHandler(
                    TcpMessageReceiver.this));
                return p;
            }
        };
    }

    protected void registerChannel(final Channel channel)
    {
        getTcpConnector().registerReceiverChannel(channel);
        receiverChannels.add(channel);
    }

    protected void fireNotification(final Channel channel, int action) {
        getTcpConnector().fireNotification(new TcpSocketNotification(channel.getId(), action));
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
    }

    @SuppressWarnings("unchecked")
    protected <T extends ChannelReceiverResource> T getChannelReceiverResource(final Channel channel,
                                                                               final Callable<T> resourceCreator)
        throws Exception
    {
        final T existing = (T) activeChannelReceiverResource.get(channel);

        if ((existing != null) && (existing.isActive()))
        {
            return existing;
        }

        final T newCis = resourceCreator.call();
        channelReceiverResourcePendingWorkerAssignment.offer(newCis);

        // even brand new, a resource can be inactive and just need to be assigned to
        // a worker, ie no further channel data will ever be routed to it
        if (newCis.isActive())
        {
            activeChannelReceiverResource.put(channel, newCis);
        }

        return newCis;
    }

    protected Work newRouterWorker(final ChannelReceiverResource crr)
    {
        return new TcpMessageReceiverRouterWorker((ChannelInputStream) crr, this);
    }

    protected Work newDispatcherWorker(final ChannelReceiverResource crr)
    {
        return new TcpRequestDispatcherWork(this, getWorkManager(), (ChannelInputStream) crr);
    }

    protected void handleException(final Exception e)
    {
        if (e instanceof MessagingException)
        {
            final MuleEvent event = ((MessagingException) e).getEvent();

            if (event != null)
            {
                event.getFlowConstruct().getExceptionListener().handleException(e, event);
                return;
            }
        }

        // don't bother with non-messaging exceptions that occur while disposing
        if (disposing.get())
        {
            return;
        }

        tcpConnector.getMuleContext().getExceptionListener().handleException(e);
    }

    protected TcpConnector getTcpConnector()
    {
        return tcpConnector;
    }

    void processMessage(AbstractTransportMessageProcessTemplate<TcpMessageReceiver, TcpConnector> messageContext) throws MuleException
    {
        super.processMessage(messageContext,messageContext);
    }
}

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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.ThreadingProfile;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connectable;
import org.mule.config.ChainedThreadingProfile;
import org.mule.transport.AbstractConnector;
import org.mule.transport.nio.tcp.i18n.TcpMessages;
import org.mule.transport.nio.tcp.io.ChannelOutputStream;
import org.mule.transport.nio.tcp.protocols.SafeProtocol;
import org.mule.util.concurrent.NamedThreadFactory;
import org.mule.util.concurrent.ThreadNameHelper;
import org.mule.util.monitor.ExpiryMonitor;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * The <code>TcpConnector</code> can bind or sent to a given TCP port on a given
 * host. Other transports can be built on top of this class by providing the
 * appropriate application level protocols as required.
 */
public class TcpConnector extends AbstractConnector
{
    public static final String PROTOCOL = "tcp";
    public static final String CONFIG_PREFIX = "niotcp";
    public static final String CHANNEL_ID_PROPERTY = "nio.channel.id";

    /** Property can be set on the endpoint to configure how the socket is managed */
    public static final int DEFAULT_SO_LINGER = INT_VALUE_NOT_SET;
    public static final int DEFAULT_BUFFER_SIZE = INT_VALUE_NOT_SET;
    public static final int DEFAULT_BACKLOG = INT_VALUE_NOT_SET;
    public static final int DEFAULT_WAIT_TIMEOUT = INT_VALUE_NOT_SET;

    protected ExecutorService bossExecutor;
    protected NioClientSocketChannelFactory dispatcherClientSocketChannelFactory;
    protected NioClientSocketChannelFactory requesterClientSocketChannelFactory;

    protected final ChannelGroup allReceiversChannels;
    protected final TcpClientFactory tcpClientFactory;
    protected final GenericKeyedObjectPool tcpClientPool;

    private int socketMaxWait = DEFAULT_WAIT_TIMEOUT;
    private int socketSoLinger = DEFAULT_SO_LINGER;
    private int sendBufferSize = DEFAULT_BUFFER_SIZE;
    private int receiveBufferSize = DEFAULT_BUFFER_SIZE;
    private int receiveBacklog = DEFAULT_BACKLOG;
    private boolean sendTcpNoDelay = false;
    private boolean reuseAddress = true;

    /**
     * Defines the receiver threading profile
     */
    private volatile ThreadingProfile receiverThreadingProfile;

    /**
     * If set, the socket is not closed after sending a message. This attribute only
     * applies when sending data over a socket (Client).
     */
    private boolean keepSendSocketOpen = false;

    /**
     * Enables SO_KEEPALIVE behavior on open sockets. This automatically checks
     * socket connections that are open but unused for long periods and closes them
     * if the connection becomes unavailable. This is a property on the socket itself
     * and is used by a server socket to control whether connections to the server
     * are kept alive before they are recycled.
     */
    private boolean keepAlive = false;

    private TcpProtocol tcpProtocol;
    private String serverGreeting;
    private int keepAliveTimeout = 0;
    private ExpiryMonitor keepAliveMonitor;

    public TcpConnector(final MuleContext context)
    {
        super(context);

        allReceiversChannels = new DefaultChannelGroup("all.receivers-channels");
        tcpClientFactory = getTcpClientFactory();
        tcpClientPool = new GenericKeyedObjectPool();
        tcpClientPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);

        tcpProtocol = new SafeProtocol();
        
        registerSupportedProtocol("tcp");
        registerSupportedProtocol(PROTOCOL);
        System.out.println("NIO TcpConnector");
    }

    protected TcpClientFactory getTcpClientFactory()
    {
        return new TcpClientFactory(this);
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        bossExecutor = Executors.newCachedThreadPool(new NamedThreadFactory(String.format("%s%s.boss",
            ThreadNameHelper.getPrefix(muleContext), getName()), muleContext.getExecutionClassLoader()));

        initialiseDispatcherClientSocketChannelFactory();
        initialiseRequesterClientSocketChannelFactory();

        initialiaseTcpClientPool();

        // Use connector's classloader so that other temporary classloaders
        // aren't used when things are started lazily or from elsewhere.
        final String monitorName = String.format("%s%s.socket", ThreadNameHelper.getPrefix(muleContext),
            getName());
        keepAliveMonitor = new ExpiryMonitor(monitorName, 1000, this.getClass().getClassLoader(),
            muleContext, false);
    }

    protected void initialiseDispatcherClientSocketChannelFactory()
    {
        final NamedThreadFactory threadFactory = new NamedThreadFactory(String.format("%s.dispatcher",
            ThreadNameHelper.dispatcher(muleContext, getName())), muleContext.getExecutionClassLoader());
        final ThreadingProfile dispatcherTp = getDispatcherThreadingProfile();

        final ExecutorService executor = new ThreadPoolExecutor(dispatcherTp.getMaxThreadsActive(),
            dispatcherTp.getMaxThreadsActive(), dispatcherTp.getThreadTTL(),
            java.util.concurrent.TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
            threadFactory, new ThreadPoolExecutor.AbortPolicy());

        dispatcherClientSocketChannelFactory = new NioClientSocketChannelFactory(bossExecutor, executor);
    }

    protected void initialiseRequesterClientSocketChannelFactory()
    {
        final NamedThreadFactory threadFactory = new NamedThreadFactory(String.format("%s.requester",
            ThreadNameHelper.requester(muleContext, getName())), muleContext.getExecutionClassLoader());
        final ThreadingProfile requesterTp = getRequesterThreadingProfile();

        final ExecutorService executor = new ThreadPoolExecutor(requesterTp.getMaxThreadsActive(),
            requesterTp.getMaxThreadsActive(), requesterTp.getThreadTTL(),
            java.util.concurrent.TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000),
            threadFactory, new ThreadPoolExecutor.AbortPolicy());

        requesterClientSocketChannelFactory = new NioClientSocketChannelFactory(bossExecutor, executor);
    }

    protected void initialiaseTcpClientPool()
    {
        tcpClientPool.setFactory(tcpClientFactory);
        tcpClientPool.setTestOnBorrow(true);
        tcpClientPool.setTestOnReturn(true);
        final int maxActive = getDispatcherThreadingProfile().getMaxThreadsActive();
        tcpClientPool.setMaxActive(maxActive);
        tcpClientPool.setMaxIdle(maxActive);
        tcpClientPool.setWhenExhaustedAction(GenericKeyedObjectPool.WHEN_EXHAUSTED_GROW);
        tcpClientPool.setMaxWait(socketMaxWait);
    }

    @Override
    public void doConnect() throws Exception
    {
        // NOOP
    }

    @Override
    public void doDisconnect() throws Exception
    {
        allReceiversChannels.close().await();
    }

    @Override
    public void doStart() throws MuleException
    {
        // NOOP
    }

    @Override
    public void doStop() throws MuleException
    {
        // NOOP
    }

    @Override
    public void doDispose()
    {
        try
        {
            tcpClientPool.close();
        }
        catch (final Exception e)
        {
            logger.warn("Failed to close TCP client socket pool: " + e.getMessage());
        }

        keepAliveMonitor.dispose();
        
        bossExecutor.shutdown();
        
        requesterClientSocketChannelFactory.releaseExternalResources();
        dispatcherClientSocketChannelFactory.releaseExternalResources();
    }

    @Override
    public ThreadingProfile getReceiverThreadingProfile()
    {
        if (receiverThreadingProfile == null)
        {
            receiverThreadingProfile = new ChainedThreadingProfile(super.getReceiverThreadingProfile())
            {
                @Override
                public int getMaxThreadsActive()
                {
                    // increment max thread active to account for the dispatcher boss
                    // worker
                    return 1 + super.getMaxThreadsActive();
                };
            };
            receiverThreadingProfile.setMuleContext(getMuleContext());
        }

        return receiverThreadingProfile;
    }

    public void registerReceiverChannel(final Channel channel)
    {
        allReceiversChannels.add(channel);
    }

    public Channel getReceiverChannel(final int id)
    {
        return allReceiversChannels.find(id);
    }

    public TcpClient borrowTcpClient(final Connectable connectable, final ImmutableEndpoint endpoint)
        throws Exception
    {
        final TcpClientKey key = newTcpClientKey(connectable, endpoint);
        final TcpClient tcpClient = (TcpClient) tcpClientPool.borrowObject(key);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("%s borrowed %s for %s keyed by %s", connectable, tcpClient,
                endpoint.getEndpointURI(), key));
        }

        return tcpClient;
    }

    public void returnTcpClient(final TcpClient tcpClient) throws Exception
    {
        final TcpClientKey key = newTcpClientKey(tcpClient.getConnectable(), tcpClient.getEndpoint());

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("%s returning to connector: %s for %s keyed by %s",
                tcpClient.getConnectable(), tcpClient, tcpClient.getEndpoint().getEndpointURI(), key));
        }

        if (!tcpClient.isKeepOpen())
        {
            tcpClientPool.invalidateObject(key, tcpClient);

            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("%s invalidated: %s for %s", tcpClient.getConnectable(),
                    tcpClient, tcpClient.getEndpoint().getEndpointURI()));
            }
        }
        else
        {
            tcpClientPool.returnObject(key, tcpClient);

            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("%s returned: %s for %s", tcpClient.getConnectable(), tcpClient,
                    tcpClient.getEndpoint().getEndpointURI()));
            }
        }
    }

    protected TcpClientKey newTcpClientKey(final Connectable connectable, final ImmutableEndpoint endpoint)
    {
        return new TcpClientKey(connectable, endpoint);
    }

    // TODO consider implementing super.getOutputStream

    public ChannelFuture write(final Object data, final Channel channel) throws IOException
    {
        final ChannelOutputStream cos = getOutputStream(channel);
        getTcpProtocol().write(cos, data);
        cos.flush();
        cos.close();
        return cos.getChannelFuture();
    }

    public static ChannelOutputStream getOutputStream(final Channel channel)
    {
        return new ChannelOutputStream(channel);
    }

    private static int valueOrDefault(final int value, final int threshhold, final int defaultValue)
    {
        if (value < threshhold)
        {
            return defaultValue;
        }
        else
        {
            return value;
        }
    }

    // getters and setters ---------------------------------------------------------

    public ExecutorService getBossExecutor()
    {
        return bossExecutor;
    }

    public NioClientSocketChannelFactory getRequesterClientSocketChannelFactory()
    {
        return requesterClientSocketChannelFactory;
    }

    public NioClientSocketChannelFactory getDispatcherClientSocketChannelFactory()
    {
        return dispatcherClientSocketChannelFactory;
    }

    public boolean isKeepAlive()
    {
        return keepAlive;
    }

    public void setKeepAlive(final boolean keepAlive)
    {
        this.keepAlive = keepAlive;
    }

    public boolean isKeepSendSocketOpen()
    {
        return keepSendSocketOpen;
    }

    public int getSocketSoLinger()
    {
        return socketSoLinger;
    }

    public void setSocketSoLinger(final int soLinger)
    {
        this.socketSoLinger = valueOrDefault(soLinger, 0, INT_VALUE_NOT_SET);
    }

    public void setKeepSendSocketOpen(final boolean keepSendSocketOpen)
    {
        this.keepSendSocketOpen = keepSendSocketOpen;
    }

    public int getSendBufferSize()
    {
        return sendBufferSize;
    }

    public void setSendBufferSize(final int bufferSize)
    {
        sendBufferSize = valueOrDefault(bufferSize, 1, DEFAULT_BUFFER_SIZE);
    }

    public String getProtocol()
    {
        return PROTOCOL;
    }

    public TcpProtocol getTcpProtocol()
    {
        return tcpProtocol;
    }

    public void setTcpProtocol(final TcpProtocol tcpProtocol)
    {
        this.tcpProtocol = tcpProtocol;
    }

    public int getSocketMaxWait()
    {
        return socketMaxWait;
    }

    public void setSocketMaxWait(final int timeout)
    {
        this.socketMaxWait = valueOrDefault(timeout, 0, DEFAULT_WAIT_TIMEOUT);
    }

    public void setServerSoTimeout(final int timeout)
    {
        logger.warn(TcpMessages.unsupportedConnectorConfigurationAttribute("connector.serverSoTimeout"));
    }

    public void setClientSoTimeout(final int timeout)
    {
        logger.warn(TcpMessages.unsupportedConnectorConfigurationAttribute("connector.clientSoTimeout"));
    }

    /**
     * @return true if the server socket sets SO_REUSEADDRESS before opening
     */
    public boolean isReuseAddress()
    {
        return reuseAddress;
    }

    /**
     * This allows closed sockets to be reused while they are still in TIME_WAIT
     * state
     * 
     * @param reuseAddress Whether the server socket sets SO_REUSEADDRESS before
     *            opening
     */
    public void setReuseAddress(final boolean reuseAddress)
    {
        this.reuseAddress = reuseAddress;
    }

    public ExpiryMonitor getKeepAliveMonitor()
    {
        return keepAliveMonitor;
    }

    /**
     * @return keep alive timeout in Milliseconds
     */
    public int getKeepAliveTimeout()
    {
        return keepAliveTimeout;
    }

    /**
     * Sets the keep alive timeout (in Milliseconds)
     */
    public void setKeepAliveTimeout(final int keepAliveTimeout)
    {
        this.keepAliveTimeout = keepAliveTimeout;
    }

    public boolean isSendTcpNoDelay()
    {
        return sendTcpNoDelay;
    }

    public void setSendTcpNoDelay(final boolean sendTcpNoDelay)
    {
        this.sendTcpNoDelay = sendTcpNoDelay;
    }

    public int getReceiveBufferSize()
    {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(final int bufferSize)
    {
        receiveBufferSize = valueOrDefault(bufferSize, 1, DEFAULT_BUFFER_SIZE);
    }

    public int getReceiveBacklog()
    {
        return receiveBacklog;
    }

    public void setReceiveBacklog(final int receiveBacklog)
    {
        this.receiveBacklog = valueOrDefault(receiveBacklog, 0, DEFAULT_BACKLOG);
    }

    public String getServerGreeting()
    {
        return serverGreeting;
    }

    public void setServerGreeting(final String serverGreeting)
    {
        this.serverGreeting = serverGreeting;
    }
}

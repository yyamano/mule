
package org.mule.transport.nio.http;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.handler.codec.http.HttpRequestEncoder;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.transport.Connectable;
import org.mule.api.transport.ReceiveException;
import org.mule.transport.nio.http.config.WebSocketEndpointConfiguration;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.transport.nio.tcp.TcpClient;
import org.mule.transport.nio.tcp.io.ChannelInputStream;
import org.mule.util.concurrent.Latch;

/**
 * A Netty powered WebSocket client. Doesn't extend {@link HttpClient} because the
 * dynamics are quite different (handshake, disconnect message...).<br/>
 * It differs from {@link HttpClient} and {@link TcpClient} in the sense that one
 * instance of it can be re-used and receive several messages. This difference is due
 * to the fact that with {@link HttpClient} and {@link TcpClient} subsequent calls to
 * <code>handleChannelData</code> provide more data to an already dispatched
 * {@link StreamableHttpRequest} (as extra chunks) or {@link ChannelInputStream} (as
 * extra bytes), while with websockets there is no such capacity to funnel more data
 * in an already dispatched {@link MuleEvent}.
 */
public class WebSocketClient extends TcpClient
{
    protected final HttpConnector httpConnector;
    protected final WebSocketEndpointConfiguration webSocketConfiguration;
    protected final BlockingQueue<WebSocketClientMessage> webSocketMessages;

    protected volatile WebSocketClientHandshaker handshaker;
    protected volatile Latch handshakeDone;

    public WebSocketClient(final HttpConnector httpConnector,
                           final Connectable connectable,
                           final ImmutableEndpoint endpoint) throws CreateException
    {
        super(httpConnector, connectable, endpoint);

        this.httpConnector = httpConnector;
        webSocketConfiguration = getWebSocketConfiguration(endpoint);
        webSocketMessages = new LinkedBlockingQueue<WebSocketClientMessage>();

        if (logger.isDebugEnabled())
        {
            logger.debug("New WebSocketClient configured with: " + webSocketConfiguration);
        }
    }

    protected WebSocketEndpointConfiguration getWebSocketConfiguration(final ImmutableEndpoint endpoint)
    {
        WebSocketEndpointConfiguration config = (WebSocketEndpointConfiguration) endpoint.getProperty(HttpConnector.PROPERTY_WEBSOCKET_CONFIG);

        if (config == null)
        {
            config = new WebSocketEndpointConfiguration(endpoint.getProperties());
        }

        return config;
    }

    @Override
    protected ChannelPipelineFactory getPipelineFactory()
    {
        return new ChannelPipelineFactory()
        {
            public ChannelPipeline getPipeline() throws Exception
            {
                final ChannelPipeline p = Channels.pipeline();
                // can't use HttpClientCodec here because a successful handshake
                // replaces HttpResponseDecoder with a WebSocket decoder
                p.addLast("netty-http-response-decoder", new HttpResponseDecoder());
                p.addLast("netty-http-response-encoder", new HttpRequestEncoder());
                p.addLast("mule-websocket-client-handler", new TcpClientUpstreamHandler(WebSocketClient.this));
                return p;
            }
        };
    }

    @Override
    public void connect() throws Exception
    {
        super.connect();
        handshaker = null;
        handshakeDone = new Latch();
    }

    protected void setUp() throws Exception
    {
        setUp(null);
    }

    @Override
    protected void setUp(final MuleEvent event) throws Exception
    {
        if (handshaker != null)
        {
            return;
        }

        handshaker = new WebSocketClientHandshakerFactory().newHandshaker(
            new URI(HttpConnector.getWebSocketAddress(endpoint)), webSocketConfiguration.getVersion(),
            webSocketConfiguration.getSubprotocols(), webSocketConfiguration.isAllowExtensions(),
            resolveHeaderExpressions(webSocketConfiguration.getHandshakeHeaders(), event),
            webSocketConfiguration.getMaxFramePayloadLength());

        handshaker.handshake(channel).awaitUninterruptibly().rethrowIfFailed();

        if (logger.isDebugEnabled())
        {
            logger.debug("Handshake initiated on: " + channel);
        }
    }

    @Override
    public void disconnect() throws Exception
    {
        if (channel.isOpen())
        {
            final ChannelFuture writeFuture = channel.write(new CloseWebSocketFrame());
            writeFuture.awaitUninterruptibly();

            if (logger.isDebugEnabled())
            {
                logger.debug("CloseWebSocketFrame written on: " + channel);
            }
        }
        super.disconnect();
    }

    protected Map<String, String> resolveHeaderExpressions(final Map<String, String> handshakeHeaders,
                                                           final MuleEvent event)
    {
        if ((handshakeHeaders == null) || (handshakeHeaders.isEmpty()))
        {
            return handshakeHeaders;
        }

        final ExpressionManager expressionManager = tcpConnector.getMuleContext().getExpressionManager();

        final Map<String, String> resolved = new HashMap<String, String>(handshakeHeaders);
        for (final Entry<String, String> header : resolved.entrySet())
        {
            final String value = header.getValue();
            if (expressionManager.isExpression(value))
            {
                @SuppressWarnings("deprecation")
                final String evaluated = (String) (event == null ? expressionManager.evaluate(value,
                    (MuleMessage) null, true) : expressionManager.evaluate(value, event, true));
                header.setValue(evaluated);
            }
        }
        return resolved;
    }

    @Override
    protected ChannelFuture doDispatch(final MuleEvent event) throws Exception
    {
        if (handshakeDone.await(getTimeout(event), TimeUnit.MILLISECONDS))
        {
            return httpConnector.writeToWebSocket(event, channel);
        }
        else
        {
            throw new MessagingException(HttpMessages.websocketHandshakeNotCompletedOnTime(), event);
        }
    }

    @Override
    protected Object buildResponseTransportMessage(final Object response)
    {
        return response;
    }

    @Override
    protected void handleChannelData(final Channel channel, final Object message) throws Exception
    {
        if (!handshaker.isHandshakeComplete())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Finishing handshake on: " + channel);
            }

            handshaker.finishHandshake(channel, (HttpResponse) message);

            if (logger.isDebugEnabled())
            {
                logger.debug("Handshake complete on: " + channel);
            }

            final WebSocketContext webSocketContext = new WebSocketContext(webSocketConfiguration, channel,
                handshaker);
            httpConnector.registerWebSocketContext(webSocketContext);

            handshakeDone.release();

            return;
        }

        if (!(message instanceof WebSocketFrame))
        {
            throw new IllegalArgumentException(String.format(
                "Can't handle channel data: %s received on channel: %s", message, channel));
        }

        final WebSocketFrame webSocketFrame = (WebSocketFrame) message;
        if (webSocketFrame instanceof PingWebSocketFrame)
        {
            channel.write(new PongWebSocketFrame((webSocketFrame).getBinaryData()));
            return;
        }

        if (webSocketFrame instanceof CloseWebSocketFrame)
        {
            handleCloseWebSocketFrame();
            return;
        }

        final WebSocketContext webSocketContext = new WebSocketContext(webSocketConfiguration, channel,
            handshaker);

        final WebSocketClientMessage webSocketMessage = new WebSocketClientMessage(webSocketFrame,
            webSocketContext);

        deliverWebSocketMessage(webSocketMessage);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Delivered: %s received from channel: %s", webSocketMessage, channel));
        }
    }

    protected void handleCloseWebSocketFrame() throws Exception
    {
        disconnect();
    }

    protected void deliverWebSocketMessage(final WebSocketClientMessage webSocketMessage)
        throws MuleException
    {
        webSocketMessages.offer(webSocketMessage);
    }

    @Override
    protected Object waitUntilResponseDelivered(final long timeout) throws Exception
    {
        // fake "forever" with Long.MAX_VALUE
        final long actualTimeOut = timeout == -1 ? Long.MAX_VALUE : timeout;

        if (logger.isDebugEnabled())
        {
            logger.debug("Waiting for response message for a maximum of: " + actualTimeOut + "ms");
        }

        final WebSocketClientMessage webSocketResponse = webSocketMessages.poll(actualTimeOut,
            TimeUnit.MILLISECONDS);

        if ((webSocketResponse == null) && (lastException != null))
        {
            throw lastException;
        }

        if (webSocketResponse == null)
        {
            throw new ReceiveException(endpoint, timeout, new TimeoutException());
        }

        return webSocketResponse;
    }
}

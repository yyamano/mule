
package org.mule.transport.nio.http;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.mule.transport.nio.http.config.WebSocketEndpointConfiguration;

/**
 * Meta-information for a websocket.
 */
public class WebSocketContext
{
    private final WebSocketEndpointConfiguration webSocketEndpointConfiguration;
    private final Channel channel;
    private final WebSocketClientHandshaker clientHandshaker;
    private final WebSocketServerHandshaker serverHandshaker;

    public WebSocketContext(final WebSocketEndpointConfiguration webSocketEndpointConfiguration,
                            final Channel channel,
                            final WebSocketServerHandshaker serverHandshaker)
    {
        Validate.notNull(webSocketEndpointConfiguration, "webSocketEndpointConfiguration can't be null");
        Validate.notNull(channel, "channel can't be null");
        Validate.notNull(serverHandshaker, "serverHandshaker can't be null");

        this.clientHandshaker = null;
        this.serverHandshaker = serverHandshaker;
        this.webSocketEndpointConfiguration = webSocketEndpointConfiguration;
        this.channel = channel;
    }

    public WebSocketContext(final WebSocketEndpointConfiguration webSocketEndpointConfiguration,
                            final Channel channel,
                            final WebSocketClientHandshaker clientHandshaker)
    {
        Validate.notNull(webSocketEndpointConfiguration, "webSocketEndpointConfiguration can't be null");
        Validate.notNull(channel, "channel can't be null");
        Validate.notNull(clientHandshaker, "clientHandshaker can't be null");

        this.clientHandshaker = clientHandshaker;
        this.serverHandshaker = null;
        this.webSocketEndpointConfiguration = webSocketEndpointConfiguration;
        this.channel = channel;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public WebSocketEndpointConfiguration getWebSocketEndpointConfiguration()
    {
        return webSocketEndpointConfiguration;
    }

    public Channel getChannel()
    {
        return channel;
    }

    public WebSocketClientHandshaker getClientHandshaker()
    {
        return clientHandshaker;
    }

    public WebSocketServerHandshaker getServerHandshaker()
    {
        return serverHandshaker;
    }
}

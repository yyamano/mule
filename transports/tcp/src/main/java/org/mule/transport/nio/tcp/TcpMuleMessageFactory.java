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

import org.jboss.netty.channel.Channel;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.transport.MuleMessageFactory;
import org.mule.transport.AbstractMuleMessageFactory;
import org.mule.transport.nio.tcp.io.ChannelInputStream;

/**
 * A {@link MuleMessageFactory} that builds {@link MuleMessage}s for each external
 * request received by the connector, either inbound or as a synchronous response to
 * an outbound interaction.
 */
public class TcpMuleMessageFactory extends AbstractMuleMessageFactory
{
    private static final Class<?>[] SUPPORTED = new Class[]{Object.class};

    public TcpMuleMessageFactory(final MuleContext context)
    {
        super(context);
    }

    @Override
    protected Class<?>[] getSupportedTransportMessageTypes()
    {
        return SUPPORTED;
    }

    @Override
    protected Object extractPayload(final Object transportMessage, final String encoding) throws Exception
    {
        return transportMessage;
    }

    @Override
    protected void addProperties(final DefaultMuleMessage message, final Object transportMessage)
        throws Exception
    {
        if (transportMessage instanceof ChannelInputStream)
        {
            final ChannelInputStream tcpMessage = (ChannelInputStream) transportMessage;
            final Channel channel = tcpMessage.getChannel();
            message.setInboundProperty(TcpConnector.CHANNEL_ID_PROPERTY, channel.getId());

            if (channel.getRemoteAddress() != null)
            {
                message.setInboundProperty(MuleProperties.MULE_REMOTE_CLIENT_ADDRESS,
                    channel.getRemoteAddress().toString());
            }
        }
    }
}

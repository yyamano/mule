/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.i18n;

import java.net.URI;

import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connectable;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.AbstractMessageReceiver;
import org.mule.transport.nio.tcp.TcpConnector;

/**
 * A {@link MessageFactory} supporting internationalized messages for the TCP
 * transport.
 */
public class TcpMessages extends MessageFactory
{
    private static final TcpMessages factory = new TcpMessages();
    private static final String BUNDLE_PATH = getBundlePath(TcpConnector.PROTOCOL);

    public static Message failedToBindToUri(final URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 1, uri);
    }

    public static Message undeliverResponseEventWhenChannelClosed()
    {
        return factory.createMessage(BUNDLE_PATH, 2);
    }

    public static Message connectAttemptTimedOut()
    {
        return factory.createMessage(BUNDLE_PATH, 3);
    }

    public static Message unexpectedRemoteResponse(final Connectable connectable, final Object message)
    {
        return factory.createMessage(BUNDLE_PATH, 4, connectable.getConnectionDescription(), message);
    }

    public static Message protocolEncodingFailed(final MuleEvent event)
    {
        return factory.createMessage(BUNDLE_PATH, 5, event);
    }

    public static Message badProtocol(final String context)
    {
        return factory.createMessage(BUNDLE_PATH, 6, context);
    }

    public static Message pollingReceiverCannotbeUsed()
    {
        return factory.createMessage(BUNDLE_PATH, 7);
    }

    public static Message failedToConnectChannelForEndpoint(final ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 8, endpoint);
    }

    public static Message errorWhileHandlingResponseFrom(final String source)
    {
        return factory.createMessage(BUNDLE_PATH, 9, source);
    }

    public static Message errorWhileHandlingRequestInReceiver(final AbstractMessageReceiver receiver)
    {
        return factory.createMessage(BUNDLE_PATH, 10, receiver.getConnectionDescription());
    }

    public static Message unsupportedConnectorConfigurationAttribute(final String name)
    {
        return factory.createMessage(BUNDLE_PATH, 11, name);
    }
}

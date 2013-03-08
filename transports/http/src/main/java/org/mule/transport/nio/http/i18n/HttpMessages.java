/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.i18n;

import java.net.URI;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.mule.api.MuleEvent;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.MessageFactory;
import org.mule.transport.nio.http.HttpConnector;

/**
 * A {@link MessageFactory} supporting internationalized messages for the HTTP
 * transport.
 */
public class HttpMessages extends MessageFactory
{
    private static final HttpMessages factory = new HttpMessages();

    private static final String BUNDLE_PATH = getBundlePath(HttpConnector.CONFIG_PREFIX);

    public static Message requestFailedWithStatus(final String string)
    {
        return factory.createMessage(BUNDLE_PATH, 3, string);
    }

    public static Message unableToGetEndpointUri(final String requestURI)
    {
        return factory.createMessage(BUNDLE_PATH, 4, requestURI);
    }

    public static Message receiverPropertyNotSet()
    {
        return factory.createMessage(BUNDLE_PATH, 7);
    }

    public static Message httpParameterNotSet(final String string)
    {
        return factory.createMessage(BUNDLE_PATH, 8, string);
    }

    public static Message malformedSyntax()
    {
        return factory.createMessage(BUNDLE_PATH, 11);
    }

    public static Message methodNotAllowed(final String method)
    {
        return factory.createMessage(BUNDLE_PATH, 12, method);
    }

    public static Message failedToConnect(final URI uri)
    {
        return factory.createMessage(BUNDLE_PATH, 13, uri);
    }

    public static Message cannotBindToAddress(final String path)
    {
        return factory.createMessage(BUNDLE_PATH, 14, path);
    }

    public static Message eventPropertyNotSetCannotProcessRequest(final String property)
    {
        return factory.createMessage(BUNDLE_PATH, 15, property);
    }

    public static Message unsupportedMethod(final String method)
    {
        return factory.createMessage(BUNDLE_PATH, 16, method);
    }

    public static Message couldNotSendExpect100()
    {
        return factory.createMessage(BUNDLE_PATH, 17);
    }

    public static Message requestLineIsMalformed(final String line)
    {
        return factory.createMessage(BUNDLE_PATH, 18, line);
    }

    public static Message pollingReceiverCannotbeUsed()
    {
        return factory.createMessage(BUNDLE_PATH, 19);
    }

    public static Message sslHandshakeDidNotComplete()
    {
        return factory.createMessage(BUNDLE_PATH, 20);
    }

    public static Message customHeaderMapDeprecated()
    {
        return factory.createMessage(BUNDLE_PATH, 21);
    }

    public static Message basicFilterCannotHandleHeader(final String header)
    {
        return factory.createMessage(BUNDLE_PATH, 22, header);
    }

    public static Message authRealmMustBeSetOnFilter()
    {
        return factory.createMessage(BUNDLE_PATH, 23);
    }

    public static Message noResourceBaseDefined()
    {
        return factory.createMessage(BUNDLE_PATH, 24);
    }

    public static Message fileNotFound(final String file)
    {
        return factory.createMessage(BUNDLE_PATH, 25, file);
    }

    public static Message failedToWriteChunkedPayload()
    {
        return factory.createMessage(BUNDLE_PATH, 26);
    }

    public static Message pollerReceivedANullResponse(final MuleEvent event)
    {
        return factory.createMessage(BUNDLE_PATH, 27, event);
    }

    public static Message webSocketContextNotFound(final Channel channel, final WebSocketFrame webSocketFrame)
    {
        return factory.createMessage(BUNDLE_PATH, 28, channel, webSocketFrame);
    }

    public static Message endpointNotConfiguredForWebSockets(final ImmutableEndpoint endpoint)
    {
        return factory.createMessage(BUNDLE_PATH, 29, endpoint);
    }

    public static Message websocketDispatchFailed()
    {
        return factory.createMessage(BUNDLE_PATH, 30);
    }

    public static Message websocketHandshakeNotCompletedOnTime()
    {
        return factory.createMessage(BUNDLE_PATH, 31);
    }
}

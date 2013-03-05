/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http;

import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.mule.transport.nio.tcp.ChannelReceiverResource;

/**
 * Defines a WebSocket message that can either be sent or received in Mule.
 */
public interface WebSocketMessage extends ChannelReceiverResource
{
    WebSocketVersion getWebSocketVersion();

    String getPath();

    Object getPayload();
}

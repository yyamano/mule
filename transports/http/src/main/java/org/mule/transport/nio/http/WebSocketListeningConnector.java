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

import java.util.Properties;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

/**
 * The {@link WebSocketListeningConnector} allows for inbound websocket endpoints to
 * be configured with an address which it connect to and listen for websocket frames.
 * If a data frame is received it becomes the inbound event for the component.
 */
public class WebSocketListeningConnector extends HttpConnector
{
    public WebSocketListeningConnector(final MuleContext context)
    {
        super(context);

        serviceOverrides = new Properties();
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS,
            WebSocketMessageReceiver.class.getName());
    }

    @Override
    public boolean isKeepSendSocketOpen()
    {
        return true;
    }
}

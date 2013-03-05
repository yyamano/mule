/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.notifications;

import org.mule.context.notification.CustomNotification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebSocketNotification extends CustomNotification
{
    private static final long serialVersionUID = -5917886397072899978L;
    protected static final Log logger = LogFactory.getLog(WebSocketNotification.class);

    public static final int UPGRADE = CUSTOM_EVENT_ACTION_START_RANGE + 1983;
    public static final int UPGRADE_DISCONNECT = UPGRADE + 1;

    private Integer channelId;

    static
    {
        registerActionSafe("socket upgrade", UPGRADE);
        registerActionSafe("socket upgrade disconnect", UPGRADE_DISCONNECT);
    }

    public WebSocketNotification(Integer channelId, int action)
    {
        super(channelId, action);
        this.channelId = channelId;
    }

    public Integer getChannelId()
    {
        return channelId;
    }

    private static void registerActionSafe(String actionName, int actionId)
    {
        try
        {
            registerAction(actionName, actionId);

        } catch(IllegalStateException ise)
        {
            // Do nothing, action has already been registered
        }
    }

}

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
import org.mule.api.transport.MessageReceiver;

/**
 * Defines a {@link Channel} resource that can be dispatched by a
 * {@link MessageReceiver}.
 */
public interface ChannelReceiverResource
{
    Channel getChannel();

    boolean isActive();
}

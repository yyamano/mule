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

import java.util.Properties;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;

/**
 * <code>PollingTcpMessageReceiver</code> acts as a polling TCP connector.
 */
public class PollingTcpConnector extends TcpConnector
{
    /**
     * The timeout to wait in milliseconds for data to come from the server.
     */
    private long timeout = 500L;

    /**
     * How long to wait in milliseconds between poll requests.
     */
    private long pollingFrequency = 1000L;

    public PollingTcpConnector(final MuleContext context)
    {
        super(context);
        serviceOverrides = new Properties();
        serviceOverrides.setProperty(MuleProperties.CONNECTOR_MESSAGE_RECEIVER_CLASS,
            PollingTcpMessageReceiver.class.getName());
    }

    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(final long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public long getTimeout()
    {
        return timeout;
    }

    public void setTimeout(final long timeout)
    {
        this.timeout = timeout;
    }
}

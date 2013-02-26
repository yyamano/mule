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

import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.lifecycle.CreateException;
import org.mule.transport.AbstractMessageRequester;

/**
 * Request transformed Mule events from TCP.
 */
public class TcpMessageRequester extends AbstractMessageRequester
{
    protected final TcpConnector tcpConnector;

    public TcpMessageRequester(final InboundEndpoint endpoint) throws CreateException
    {
        super(endpoint);
        tcpConnector = (TcpConnector) connector;
    }

    @Override
    protected MuleMessage doRequest(final long timeout) throws Exception
    {
        final TcpClient tcpClient = tcpConnector.borrowTcpClient(this, endpoint);
        return tcpClient.request(timeout);
    }
}

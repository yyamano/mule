/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.functional;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConnector;

public class ChunkingTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "chunking-test.xml";
    }

    @Test
    public void testPartiallyReadRequest() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        final byte[] msg = new byte[100 * 1024];

        MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inMain")).getAddress(), msg, null);
        assertEquals("Hello", result.getPayloadAsString());
        String status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        assertEquals("200", status);

        result = client.send(
            ((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("inMain")).getAddress(),
            msg, null);
        assertEquals("Hello", result.getPayloadAsString());
        status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        assertEquals("200", status);
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class SynchStreamingMule1687TestCase extends FunctionalTestCase
{
    public static final String TEST_MESSAGE = "Test TCP Request";

    @Rule
    public NioProperty nio = new NioProperty(true);

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "nio/tcp-synch-streaming-test.xml";
    }

    @Test
    public void testSendAndRequest() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final ByteArrayInputStream stream = new ByteArrayInputStream(TEST_MESSAGE.getBytes());
        final MuleMessage request = new DefaultMuleMessage(stream, muleContext);
        final MuleMessage message = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inEcho")).getAddress(), request);
        assertNotNull(message);

        final Object payload = message.getPayload();
        assertTrue(payload instanceof InputStream);
        assertEquals("Test TCP Request", message.getPayloadAsString());
    }

}

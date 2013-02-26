/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.integration;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class CustomByteProtocolTestCase extends FunctionalTestCase
{
    final private int messages = 100;

    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "nio/custom-byte-protocol-mule-config.xml";
    }

    @Test
    public void testCustomProtocol() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final String testPayload = "Hello";

        for (int i = 0; i < messages; i++)
        {
            client.dispatch("vm://in", testPayload, null);
        }

        for (int i = 0; i < messages; i++)
        {
            final MuleMessage msg = client.request("vm://out", 30000);
            assertEquals(testPayload, msg.getPayloadAsString());
        }
    }
}

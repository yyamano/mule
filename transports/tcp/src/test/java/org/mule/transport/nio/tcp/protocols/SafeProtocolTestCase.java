/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.protocols;

import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.tcp.protocols.AbstractSafeProtocolTestCase;

import org.junit.Rule;
import org.junit.Test;

@NioTest
public class SafeProtocolTestCase extends AbstractSafeProtocolTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    public SafeProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test(expected = Exception.class)
    public void testUnsafeToSafe() throws MuleException
    {
        final MuleClient client = new MuleClient(muleContext);
        client.send("tcp://localhost:" + dynamicPort1.getNumber() + "?connector=unsafe", TEST_MESSAGE, null);
    }
}



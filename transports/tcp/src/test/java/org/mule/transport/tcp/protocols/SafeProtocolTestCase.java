/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.protocols;

import static org.junit.Assert.fail;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.tcp.LegacyIoTest;

import org.junit.Test;

@LegacyIoTest
public class SafeProtocolTestCase extends AbstractSafeProtocolTestCase
{

    public SafeProtocolTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testUnsafeToSafe() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        assertResponseBad(client.send("tcp://localhost:" + dynamicPort1.getNumber() + "?connector=unsafe",
            TEST_MESSAGE, null));
    }

    protected void assertResponseBad(MuleMessage message)
    {
        try
        {
            if (message.getPayloadAsString().equals(TEST_MESSAGE + " Received"))
            {
                fail("expected error");
            }
        }
        catch (Exception e)
        {
            // expected
        }
    }

}



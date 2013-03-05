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
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConnector;

public class HttpEndpointConstructTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-endpoint-construct-conf.xml";
    }

    @Test
    public void testHttpEndpointConstruct() throws Exception
    {
        final MuleClient client = muleContext.getClient();
        final MuleMessage response = client.send(HttpConnector.HTTP + "://localhost:" + dynamicPort1.getNumber() + "/testA",
            TEST_MESSAGE, null);
        assertEquals(TEST_MESSAGE, response.getPayloadAsString());
    }
}

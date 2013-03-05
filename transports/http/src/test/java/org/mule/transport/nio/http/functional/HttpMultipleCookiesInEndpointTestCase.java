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
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class HttpMultipleCookiesInEndpointTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpMultipleCookiesInEndpointTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "nio/http-multiple-cookies-on-endpoint-test-service.xml"},
            {ConfigVariant.FLOW, "nio/http-multiple-cookies-on-endpoint-test-flow.xml"}});
    }

    @Test
    public void testThatThe2CookiesAreSentAndReceivedByTheComponent() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage response = client.send("vm://in", "HELLO", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Both Cookies Found!", response.getPayloadAsString());
    }
}

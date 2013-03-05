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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.expression.RequiredValueException;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class HttpFunctionalWithQueryTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpFunctionalWithQueryTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-functional-test-with-query-service.xml"},
            {ConfigVariant.FLOW, "http-functional-test-with-query-flow.xml"}});
    }

    @Test
    public void testSend() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.send("clientEndpoint1", null, null);
        assertEquals("boobar", result.getPayloadAsString());
    }

    @Test
    public void testSendWithParams() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("foo", "noo");
        props.put("far", "nar");
        final MuleMessage result = client.send("clientEndpoint2", null, props);
        assertEquals("noonar", result.getPayloadAsString());
    }

    @Test
    public void testSendWithBadParams() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put("hoo", "noo");
        props.put("har", "nar");

        try
        {
            client.send("clientEndpoint2", null, props);
            fail("Required values missing");
        }
        catch (final Exception e)
        {
            assertTrue(e.getCause() instanceof RequiredValueException);
        }
    }
}

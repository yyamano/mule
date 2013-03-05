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
import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpPollingConnector;

public class HttpPollingFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpPollingFunctionalTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "mule-http-polling-config-service.xml"},
            {ConfigVariant.FLOW, "mule-http-polling-config-flow.xml"}});
    }

    @Override
    protected void doTearDown() throws Exception
    {
        // Mule may shut down polled resources before pollers stop leading to
        // unnecessary errors -> stop pollers beforehand to avoid that
        stopHttpPollingConnectors(muleContext);
    }

    public static void stopHttpPollingConnectors(final MuleContext muleContext) throws MuleException
    {
        for (final HttpPollingConnector httpPollingConnector : muleContext.getRegistry().lookupObjects(
            HttpPollingConnector.class))
        {
            httpPollingConnector.stop();
        }
    }

    @Test
    public void testPollingHttpConnector() throws Exception
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent("polled");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component)
                throws Exception
            {
                assertEquals("The Accept header should be set on the incoming message", "application/xml",
                    context.getMessage().<String> getInboundProperty("Accept"));
            }
        });
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.request("vm://toclient", RECEIVE_TIMEOUT);
        assertNotNull(result.getPayload());
        assertEquals("foo", result.getPayloadAsString());
    }
}

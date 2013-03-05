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
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;

public class HttpFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";
    protected boolean checkPathProperties = true;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpFunctionalTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "http-functional-test-service.xml"},
            {ConfigVariant.FLOW, "http-functional-test-flow.xml"}});
    }

    @Test
    public void testSend() throws Exception
    {
        final FunctionalTestComponent testComponent = getFunctionalTestComponent("testComponent");
        assertNotNull(testComponent);

        if (checkPathProperties)
        {
            final EventCallback callback = new EventCallback()
            {
                public void eventReceived(final MuleEventContext context, final Object component)
                    throws Exception
                {
                    final MuleMessage msg = context.getMessage();
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                    assertEquals("/", msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
                }
            };

            testComponent.setEventCallback(callback);
        }

        final MuleClient client = new MuleClient(muleContext);
        final Map<String, String> props = new HashMap<String, String>();
        props.put(HttpConstants.HEADER_CONTENT_TYPE, "text/plain;charset=UTF-8");
        final MuleMessage result = client.send("clientEndpoint", TEST_MESSAGE, props);
        assertEquals(TEST_MESSAGE + " Received", result.getPayloadAsString());
    }

}

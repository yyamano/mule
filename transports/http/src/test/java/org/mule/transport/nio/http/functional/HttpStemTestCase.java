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

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConnector;

public class HttpStemTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-stem-test.xml";
    }

    @Test
    public void testStemMatching() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final int port = dynamicPort.getNumber();
        doTest(client, HttpConnector.HTTP + "://localhost:" + port + "/foo", "/foo", "/foo");
        doTest(client, HttpConnector.HTTP + "://localhost:" + port + "/foo/baz", "/foo", "/foo/baz");
        doTest(client, HttpConnector.HTTP + "://localhost:" + port + "/bar", "/bar", "/bar");
        doTest(client, HttpConnector.HTTP + "://localhost:" + port + "/bar/baz", "/bar", "/bar/baz");
    }

    protected void doTest(final MuleClient client,
                          final String url,
                          final String contextPath,
                          final String requestPath) throws Exception
    {
        final FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(contextPath);
        assertNotNull(testComponent);

        final EventCallback callback = new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component)
                throws Exception
            {
                final MuleMessage msg = context.getMessage();
                assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PROPERTY));
                assertEquals(requestPath, msg.getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY));
                assertEquals(contextPath, msg.getInboundProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY));
            }
        };

        testComponent.setEventCallback(callback);

        final MuleMessage result = client.send(url, "Hello World", null);
        assertEquals("Hello World Received", result.getPayloadAsString());
        final String status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        assertEquals("200", status);
    }

}

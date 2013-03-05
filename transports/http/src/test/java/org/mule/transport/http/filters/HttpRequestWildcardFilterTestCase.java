/*
 * $Id: HttpRequestWildcardFilterTestCase.java 22518 2011-07-22 07:00:22Z claude.mamo $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;

public class HttpRequestWildcardFilterTestCase extends AbstractServiceAndFlowTestCase
{
    private static final String TEST_HTTP_MESSAGE = "Hello=World";
    private static final String TEST_BAD_MESSAGE = "xyz";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public HttpRequestWildcardFilterTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.FLOW, "http-wildcard-filter-test-flow.xml"},
            {ConfigVariant.SERVICE, "http-wildcard-filter-test-service.xml"}});
    }

    @Test
    public void testReference() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inReference")).getAddress(), TEST_HTTP_MESSAGE, null);

        assertEquals(TEST_HTTP_MESSAGE, result.getPayloadAsString());
    }

    @Test
    public void testHttpPost() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inHttpIn")).getAddress(), TEST_HTTP_MESSAGE, null);

        assertEquals(TEST_HTTP_MESSAGE, result.getPayloadAsString());
    }

    @Test
    public void testHttpGetNotFiltered() throws Exception
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConstants.METHOD_GET, "true");

        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inHttpIn")).getAddress()
                                               + "/" + "mulerulez", TEST_HTTP_MESSAGE, props);

        assertEquals(TEST_HTTP_MESSAGE, result.getPayloadAsString());
    }

    @Test
    public void testHttpGetFiltered() throws Exception
    {
        final Map<String, Object> props = new HashMap<String, Object>();
        props.put(HttpConnector.HTTP_METHOD_PROPERTY, HttpConstants.METHOD_GET);

        final MuleClient client = new MuleClient(muleContext);
        final MuleMessage result = client.send(((InboundEndpoint) client.getMuleContext()
            .getRegistry()
            .lookupObject("inHttpIn")).getAddress()
                                               + "/" + TEST_BAD_MESSAGE, "mule", props);

        final String status = result.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY);
        assertEquals("406", status); // SC_NOT_ACCEPTABLE
        assertNotNull(result.getExceptionPayload());
    }

}

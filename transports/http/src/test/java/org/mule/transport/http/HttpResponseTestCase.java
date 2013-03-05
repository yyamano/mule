/*
 * $Id: HttpResponseTestCase.java 24155 2012-03-23 20:47:12Z evangelinamrm $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;


public class HttpResponseTestCase extends FunctionalTestCase
{
    private static final String HTTP_BODY = "<html><head></head><body><p>This is the response body</p></body></html>";

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "http-response-conf.xml";
    }

    @Test
    public void testHttpResponseError() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("errorMessage", "ERROR !!!! ");
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/error", muleMessage, properties);
        assertTrue(response.getPayloadAsString().contains("ERROR !!!!"));
        assertEquals(Integer.toString(HttpConstants.SC_INTERNAL_SERVER_ERROR), response.getInboundProperty("http.status"));
    }

    @Test
    public void testHttpResponseMove() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/move", muleMessage);
        assertEquals(HTTP_BODY, response.getPayloadAsString());
        assertEquals(Integer.toString(HttpConstants.SC_MOVED_PERMANENTLY), response.getInboundProperty("http.status"));
        assertEquals("http://localhost:9090/resources/moved", response.<Object>getInboundProperty("Location"));
    }
}

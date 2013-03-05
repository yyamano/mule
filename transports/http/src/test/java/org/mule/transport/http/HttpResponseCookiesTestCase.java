/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.tcp.LegacyIoTest;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.Cookie;
import org.junit.Rule;
import org.junit.Test;

@LegacyIoTest
public class HttpResponseCookiesTestCase extends FunctionalTestCase
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
    public void testHttpResponseAll() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/all", muleMessage);
        assertEquals("Custom body", response.getPayloadAsString());
        assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
        assertEquals("public,no-cache,must-revalidate,max-age=3600,no-transform", response.getInboundProperty("Cache-Control"));
        assertEquals("Fri, 01 Dec 2079 16:00:00 GMT", response.getInboundProperty("Expires"));
        assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
        assertEquals("value1", response.getInboundProperty("header1"));
        Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
        assertEquals(2, cookies.length);
        validateCookie(cookies[0]);
        validateCookie(cookies[1]);
    }

    @Test
    public void testHttpResponseAllWithExpressions() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> properties = populateProperties();

        DefaultMuleMessage muleMessage = new DefaultMuleMessage(HTTP_BODY, properties, muleContext);
        MuleMessage response = client.send("http://localhost:" + dynamicPort.getNumber() + "/resources/allExpressions",  muleMessage, properties);
        assertEquals("" + HttpConstants.SC_NOT_FOUND, response.getInboundProperty("http.status"));
        assertEquals("max-age=3600", response.getInboundProperty("Cache-Control"));
        assertEquals("Fri, 01 Dec 2079 16:00:00 GMT", response.getInboundProperty("Expires"));
        assertEquals("http://localhost:9090", response.getInboundProperty("Location"));
        assertEquals("value1", response.getInboundProperty("header1"));
        Cookie[] cookies = (Cookie[]) response.getInboundProperty("Set-Cookie");
        assertEquals(2, cookies.length);
        validateCookie(cookies[0]);
        validateCookie(cookies[1]);
    }

    private Map<String, Object> populateProperties()
    {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("customBody", "Custom body");
        properties.put("contentType", "text/html");
        properties.put("status", HttpConstants.SC_NOT_FOUND);
        properties.put("cacheControl", "3600");
        properties.put("expires", "Fri, 01 Dec 2079 16:00:00 GMT");
        properties.put("location", "http://localhost:9090");
        properties.put("header1", "header1");
        properties.put("header2", "header2");
        properties.put("value1", "value1");
        properties.put("value2", "value2");
        properties.put("cookie1", "cookie1");
        properties.put("cookie2", "cookie2");
        properties.put("domain", "localhost");
        properties.put("path", "/");
        properties.put("secure", true);
        properties.put("expiryDate", "Sat, 02 Dec 2079 17:00:00 GMT");
        properties.put("maxAge", "1000");
        return properties;

    }

    private void validateCookie(Cookie cookie)
    {
        if("cookie1".equals(cookie.getName()))
        {
            assertEquals("value1", cookie.getValue());
            assertEquals("/", cookie.getPath());
            assertEquals("localhost", cookie.getDomain());
            validateDate(cookie.getExpiryDate());
            assertTrue(cookie.getSecure());
        }
        else
        {
            assertEquals("cookie2", cookie.getName());
            assertEquals("value2", cookie.getValue());
            assertFalse(cookie.getSecure());
        }
    }

    private void validateDate(Date date)
    {
        GregorianCalendar cookieDate = new GregorianCalendar();
        cookieDate.setTime(date);

        assertEquals(2079, cookieDate.get(GregorianCalendar.YEAR));
        assertEquals(2, cookieDate.get(GregorianCalendar.DAY_OF_MONTH));
        assertEquals(11, cookieDate.get(GregorianCalendar.MONTH));
    }

}



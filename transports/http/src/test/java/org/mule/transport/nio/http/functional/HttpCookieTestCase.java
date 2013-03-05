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

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

public class HttpCookieTestCase extends AbstractMockHttpServerTestCase
{
    private static final String COOKIE_HEADER = "Cookie:";

    private final CountDownLatch latch = new CountDownLatch(1);
    private boolean cookieFound = false;
    private final List<Cookie> cookies = new ArrayList<Cookie>();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpCookieTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "http-cookie-test-service.xml"},
            {ConfigVariant.FLOW, "http-cookie-test-flow.xml"}});
    }

    @Override
    protected MockHttpServer getHttpServer(final CountDownLatch serverStartLatch)
    {
        return new SimpleHttpServer(dynamicPort.getNumber(), serverStartLatch, latch);
    }

    @Test
    public void testCookies() throws Exception
    {
        final Map<String, String> properties = new HashMap<String, String>();
        properties.put("COOKIE_HEADER", "MYCOOKIE");

        final MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://vm-in", "foobar", properties);

        assertTrue(latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(cookieFound);

        assertEquals(2, cookies.size());
        assertCookiePresence("customCookie", "yes");
        assertCookiePresence("expressionCookie", "MYCOOKIE");
    }

    private void assertCookiePresence(final String name, final String value)
    {
        for (final Cookie cookie : cookies)
        {
            if (org.apache.commons.lang.StringUtils.equals(cookie.getName(), name)
                && org.apache.commons.lang.StringUtils.equals(cookie.getValue(), value))
            {
                return;
            }
        }
        fail("There should be a cookie named: " + name + " with value: " + value);
    }

    private class SimpleHttpServer extends MockHttpServer
    {
        public SimpleHttpServer(final int listenPort,
                                final CountDownLatch startupLatch,
                                final CountDownLatch testCompleteLatch)
        {
            super(listenPort, startupLatch, testCompleteLatch);
        }

        @Override
        protected void readHttpRequest(final BufferedReader reader) throws Exception
        {
            String line = reader.readLine();
            while (line != null)
            {
                // Check that we receive a 'Cookie:' header as it would be
                // send by a regular http client
                if (line.indexOf(COOKIE_HEADER) > -1)
                {
                    cookieFound = true;
                    cookies.addAll(new CookieDecoder(true).decode(org.apache.commons.lang.StringUtils.substringAfter(
                        line, COOKIE_HEADER)
                        .trim()));
                }

                line = reader.readLine();
                // only read the header, i.e. if we encounter an empty line
                // stop reading (we're only interested in the headers anyway)
                if (line.trim().length() == 0)
                {
                    line = null;
                }
            }
        }
    }
}

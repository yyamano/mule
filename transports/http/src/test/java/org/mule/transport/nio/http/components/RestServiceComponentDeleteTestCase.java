/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.components;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.functional.AbstractMockHttpServerTestCase;
import org.mule.transport.nio.http.functional.MockHttpServer;

public class RestServiceComponentDeleteTestCase extends AbstractMockHttpServerTestCase
{
    private final CountDownLatch serverRequestCompleteLatch = new CountDownLatch(1);
    private boolean deleteRequestFound = false;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public RestServiceComponentDeleteTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
        System.setProperty("java.protocol.handler.pkgs", "org.mule.transport");
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "rest-service-component-delete-test-service.xml"},
            {ConfigVariant.FLOW, "rest-service-component-delete-test-flow.xml"}});
    }

    @Override
    protected MockHttpServer getHttpServer(final CountDownLatch serverStartLatch)
    {
        return new SimpleHttpServer(dynamicPort.getNumber(), serverStartLatch, serverRequestCompleteLatch);
    }

    @Test
    public void testRestServiceComponentDelete() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);
        client.send("vm://fromTest", TEST_MESSAGE, null);

        assertTrue(serverRequestCompleteLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(deleteRequestFound);
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
            final String requestLine = reader.readLine();
            final String httpMethod = new StringTokenizer(requestLine).nextToken();

            deleteRequestFound = httpMethod.equals(HttpConstants.METHOD_DELETE);
        }
    }
}

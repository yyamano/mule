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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class HttpMultipleCookiesTestCase extends AbstractServiceAndFlowTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request ";
    protected static final Log logger = LogFactory.getLog(HttpMultipleCookiesTestCase.class);

    private final CountDownLatch simpleServerLatch = new CountDownLatch(1);
    private final CountDownLatch simpleServerShutdownLatch = new CountDownLatch(1);
    private static AtomicBoolean cookiesReceived = new AtomicBoolean(false);

    private final Server server = null;

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public HttpMultipleCookiesTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "http-multiple-cookies-test-service.xml"},
            {ConfigVariant.FLOW, "http-multiple-cookies-test-flow.xml"}});
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        startServer();
        assertTrue(simpleServerLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Override
    protected void doTearDown() throws Exception
    {
        super.doTearDown();
        muleContext.stop();
        stopServer();
        assertTrue(simpleServerShutdownLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSendDirectly() throws Exception
    {
        muleContext.start();
        sendMessage(dynamicPort2.getNumber());
    }

    @Test
    public void testSendViaMule() throws Exception
    {
        muleContext.start();
        sendMessage(dynamicPort1.getNumber());
    }

    protected void sendMessage(final int port) throws Exception
    {
        final HttpClient client2 = new HttpClient();
        client2.getParams().setCookiePolicy(CookiePolicy.RFC_2109);
        final HttpState state = new HttpState();
        final Cookie cookie1 = new Cookie("localhost", "TheFirst", "First", "/", null, false);
        state.addCookie(cookie1);
        final Cookie cookie2 = new Cookie("localhost", "TheSecond", "Value2", "/", null, false);
        state.addCookie(cookie2);
        final Cookie cookie3 = new Cookie("localhost", "TheThird", "Value3", "/", null, false);
        state.addCookie(cookie3);

        client2.setState(state);
        final PostMethod method = new PostMethod("http://localhost:" + port);
        method.setRequestEntity(new StringRequestEntity("test", "text/plain", "UTF-8"));
        client2.executeMethod(method);

        assertEquals(TEST_MESSAGE, method.getResponseBodyAsString());
        assertTrue("Cookies were not received", cookiesReceived.get());

        for (final Cookie cookie : client2.getState().getCookies())
        {
            logger.debug(cookie.getName() + " " + cookie.getValue());
        }
        assertEquals(6, client2.getState().getCookies().length);
    }

    protected void startServer() throws Exception
    {
        logger.debug("server starting");
        final Server server = new Server();
        final Connector connector = new SocketConnector();
        connector.setPort(dynamicPort2.getNumber());
        server.setConnectors(new Connector[]{connector});

        final ServletHandler handler = new ServletHandler();
        server.setHandler(handler);

        handler.addServletWithMapping(HelloServlet.class.getName(), "/");

        server.start();
        // server.join();
        simpleServerLatch.countDown();
        logger.debug("Server started");
    }

    protected void stopServer() throws Exception
    {
        logger.debug("server stopping");

        if (server != null && server.isRunning())
        {
            assertEquals(1, server.getConnectors());
            // this test only uses one connector
            server.getConnectors()[0].stop();
        }

        simpleServerShutdownLatch.countDown();
        logger.debug("Server stopped");
    }

    public static class HelloServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;

        @Override
        protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
        {
            try
            {
                response.setContentType("text/xml");
                response.setContentLength(TEST_MESSAGE.length());
                for (int i = 0; i < 3; i++)
                {
                    final javax.servlet.http.Cookie cookie1 = new javax.servlet.http.Cookie(
                        "OutputCookieName" + i, "OutputCookieValue" + i);
                    response.addCookie(cookie1);
                }
                cookiesReceived.set(false);
                final javax.servlet.http.Cookie[] cookies = request.getCookies();
                if (cookies != null)
                {
                    for (final javax.servlet.http.Cookie cookie : cookies)
                    {
                        logger.debug(cookie.getName() + " " + cookie.getValue());
                        cookiesReceived.set(true);
                    }
                }
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(TEST_MESSAGE);
            }
            catch (final Exception e)
            {
                logger.error("Servlet error", e);
                throw new ServletException(e);
            }
        }

        @Override
        protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException
        {
            doGet(request, response);
        }
    }
}

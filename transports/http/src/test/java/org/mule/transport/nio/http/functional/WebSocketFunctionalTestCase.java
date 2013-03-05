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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpPollingConnector;
import org.mule.transport.nio.http.WebSocketListeningConnector;
import org.mule.transport.nio.tcp.NioTest;

import com.ning.http.client.Response;

@NioTest
public class WebSocketFunctionalTestCase extends AbstractWebSocketFunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "nio/websocket-functional-conf.xml";
    }

    @Test
    public void testStaticResource() throws Exception
    {
        final Future<Response> f = httpClient.prepareGet(getBaseHttpUrl().replace(HttpConnector.PROTOCOL, "http") + "/websocket-client.html")
            .execute();
        final Response r = f.get();

        Object body = r.getResponseBody();
        assertNotNull(body);
        assertTrue(body.toString().contains("WebSocket.OPEN"));
    }

    @Test
    public void testRequestResponseWebSocketNoVersion() throws Exception
    {
        doTestRequestResponseWebSocket(null);
    }

    @Test
    public void testRequestResponseWebSocketV13() throws Exception
    {
        doTestRequestResponseWebSocket("V13");
    }

    @Test
    public void testRequestResponseWebSocketV08() throws Exception
    {
        doTestRequestResponseWebSocket("V8");
    }

    @Test
    public void testRequestResponseWebSocketV00() throws Exception
    {
        doTestRequestResponseWebSocket("V00");
    }

    @Test
    public void testOneWayWebSocket() throws Exception
    {
        final BlockingQueue<String> resultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");

        final String testMessage = RandomStringUtils.randomAlphanumeric(10);

        sendMessageToWebSocket(testMessage, "/ow");

        assertReceived(resultQueue, testMessage + " received on /ow");
    }

    @Test
    public void testGetBroadcastMessageFromWebSocket() throws Exception
    {
        assertThat(getMessageFromWebSocket("/ow"), is("General Ping"));
    }

    @Test
    public void testSimulatedResponseViaDirectWebSocketWrite() throws Exception
    {
        doTestRequestResponseWebSocket(null, "/sw");
    }

    @Test
    public void testSimulatedResponseViaCodedWebSocketWrite() throws Exception
    {
        doTestRequestResponseWebSocket(null, "/code-sw");
    }

    @Test
    public void testOutboundCloseSendSocket() throws Exception
    {
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);

        final MuleMessage response = muleContext.getClient().send("vm://ws.out.close", testMessage, null,
            getTestTimeoutSecs() * 1000);

        assertThat(response.getPayloadAsString(), is(testMessage + " received on /rr"));
    }

    @Test
    public void testOutboundKeepSendSocketOpen() throws Exception
    {
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);

        final MuleMessage response = muleContext.getClient().send("vm://ws.out.keep-open", testMessage, null,
            getTestTimeoutSecs() * 1000);

        assertThat(response.getPayloadAsString(), is(testMessage + " received on /rr"));
    }

    @Test
    public void testMuleClientSendCloseSendSocket() throws Exception
    {
        doTestMuleClientSendClose("httpConnector", "testMuleClientSendCloseSendSocket");
    }

    @Test
    public void testMuleClientSendKeepSendSocketOpen() throws Exception
    {
        doTestMuleClientSendClose("httpConnectorKeepOpen", "testMuleClientSendKeepSendSocketOpen");
    }

    @Test
    public void testMuleClientDispatchCloseSendSocket() throws Exception
    {
        doTestMuleClientDispatch("httpConnector", "testMuleClientDispatchCloseSendSocket");
    }

    @Test
    public void testMuleClientDispatchKeepSendSocketOpen() throws Exception
    {
        doTestMuleClientDispatch("httpConnectorKeepOpen", "testMuleClientDispatchKeepSendSocketOpen");
    }

    @Test
    public void testMuleClientRequestCloseSendSocket() throws Exception
    {
        doTestMuleClientRequest("httpConnector");
    }

    @Test
    public void testMuleClientRequestKeepSendSocketOpen() throws Exception
    {
        doTestMuleClientRequest("httpConnectorKeepOpen");
    }

    @Test
    public void testPollingWebsocketClosing() throws Exception
    {
        final BlockingQueue<String> clientResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-polling");
        assertReceived(clientResultQueue, "General Ping");
    }

    @Test
    public void testPollingWebsocketKeepOpen() throws Exception
    {
        // first ensure we can poll messages
        final BlockingQueue<String> clientResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-polling-keep-open");
        assertThat(pollResult(clientResultQueue), is("General Ping"));

        // then test we can write to the open inbound websocket
        final BlockingQueue<String> serverResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);
        final HttpPollingConnector httpPollingConnector = (HttpPollingConnector) muleContext.getRegistry()
            .lookupConnector("pollingHttpConnectorKeepOpen");
        httpPollingConnector.writeToWebSocket(new DefaultMuleMessage(testMessage, muleContext),
            "client-websocket");

        assertReceived(serverResultQueue, testMessage + " received on /ow");
    }

    @Test
    public void testListeningWebsocketOneWay() throws Exception
    {
        // first ensure we can listen to messages
        final BlockingQueue<String> clientResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-listening-ow");
        assertThat(pollResult(clientResultQueue), is("General Ping"));

        // then test we can write to the open inbound websocket
        final BlockingQueue<String> serverResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);
        final WebSocketListeningConnector webSocketListeningConnector = (WebSocketListeningConnector) muleContext.getRegistry()
            .lookupConnector("listeningWebSocketConnectorOW");
        webSocketListeningConnector.writeToWebSocket(new DefaultMuleMessage(testMessage, muleContext),
            "listener-websocket");

        assertReceived(serverResultQueue, testMessage + " received on /ow");
    }

    @Test
    public void testListeningWebsocketRequestResponse() throws Exception
    {
        // first ensure we can listen to messages
        final BlockingQueue<String> clientResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-listening-rr");
        assertReceived(clientResultQueue, "General Ping");

        // then test the client synchronous response was received by the server
        final BlockingQueue<String> serverResultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");
        assertReceived(serverResultQueue, "General Ping received on /listening-rr received on /ow");
    }

    protected void assertReceived(final BlockingQueue<String> resultQueue, final String expected)
        throws InterruptedException
    {
        String polled = null;
        while ((polled = pollResult(resultQueue)) != null)
        {
            if (StringUtils.equals(polled, expected))
            {
                return;
            }
        }

        fail("Never received: " + expected);
    }

    protected void doTestMuleClientSendClose(final String httpConnectorName, final String testName)
        throws Exception
    {
        final String testMessage = testName + "-" + RandomStringUtils.randomAlphanumeric(10);

        final MuleMessage response = muleContext.getClient().send(
            getWebSocketUrl("/rr") + "?connector=" + httpConnectorName, testMessage, null,
            getTestTimeoutSecs() * 1000);

        assertThat(response.getPayloadAsString(), is(testMessage + " received on /rr"));
    }

    protected void doTestMuleClientDispatch(final String httpConnectorName, final String testName)
        throws Exception
    {
        final BlockingQueue<String> resultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");
        final String testMessage = testName + "-" + RandomStringUtils.randomAlphanumeric(10);

        muleContext.getClient().dispatch(getWebSocketUrl("/ow") + "?connector=" + httpConnectorName,
            testMessage, null);

        assertReceived(resultQueue, testMessage + " received on /ow");
    }

    protected void doTestMuleClientRequest(final String httpConnectorName) throws Exception
    {
        MuleMessage requested = null;

        while ((requested = muleContext.getClient().request(
            getWebSocketUrl("/ow") + "?connector=" + httpConnectorName, getTestTimeoutSecs() * 1000)) != null)
        {
            if (StringUtils.contains(requested.getPayloadAsString(), "General Ping"))
            {
                return;
            }
        }
        fail("Never received expected answer");
    }

    protected void doTestRepeatedOps(final WebSocketTestClient wstc,
                                     final String webSocketGroup,
                                     final String testMethodName) throws Exception
    {
        final int numberOfMessages = 10;
        final int numberOfClients = 10;

        final BlockingQueue<String> resultQueue = setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");

        final Set<String> expectedResults = new ConcurrentSkipListSet<String>();

        for (int i = 0; i < numberOfClients; i++)
        {
            final Runnable clientRunnable = new Runnable()
            {
                public void run()
                {
                    try
                    {
                        for (int j = 0; j < numberOfMessages; j++)
                        {
                            final String testMessage = Thread.currentThread().getName() + "-"
                                                       + testMethodName + j;
                            expectedResults.add(testMessage + " received on " + webSocketGroup);
                            wstc.sendMessage(testMessage, resultQueue);
                        }
                    }
                    catch (final Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            };

            new Thread(clientRunnable, "ws-client-" + i).start();
        }

        while (!expectedResults.isEmpty())
        {
            final String result = pollResult(resultQueue);
            expectedResults.remove(result);
        }
    }

    private void doTestRequestResponseWebSocket(final String webSocketProtocol) throws Exception
    {
        doTestRequestResponseWebSocket(webSocketProtocol, "/rr");
    }

    private void doTestRequestResponseWebSocket(final String webSocketProtocol, final String webSocketGroup)
        throws Exception
    {
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);

        final BlockingQueue<String> resultQueue = sendMessageToWebSocket(testMessage, webSocketGroup,
            webSocketProtocol);

        assertThat(pollResult(resultQueue), is(testMessage + " received on " + webSocketGroup));
    }

    private String getMessageFromWebSocket(final String webSocketGroup) throws Exception
    {
        return sendMessageToWebSocket(null, webSocketGroup, null).poll(getTestTimeoutSecs(), TimeUnit.SECONDS);
    }

    private BlockingQueue<String> sendMessageToWebSocket(final String testMessage, final String webSocketGroup)
        throws Exception
    {
        return sendMessageToWebSocket(testMessage, webSocketGroup, null);
    }

    private BlockingQueue<String> sendMessageToWebSocket(final String testMessage,
                                                         final String webSocketGroup,
                                                         final String webSocketProtocol) throws Exception
    {
        final BlockingQueue<String> resultQueue = new LinkedBlockingQueue<String>();
        sendMessageToWebSocket(testMessage, webSocketGroup, webSocketProtocol, resultQueue);
        return resultQueue;
    }

    private String getBaseHttpUrl()
    {
        return HttpConnector.HTTP + "://localhost:" + getDynamicPort1();
    }
}

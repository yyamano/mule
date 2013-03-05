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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.Rule;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.util.ClassUtils;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.websocket.DefaultWebSocketListener;
import com.ning.http.client.websocket.WebSocket;
import com.ning.http.client.websocket.WebSocketUpgradeHandler;

@NioTest
public abstract class AbstractWebSocketFunctionalTestCase extends FunctionalTestCase
{
    protected static interface WebSocketTestClient
    {
        void sendMessage(String message, BlockingQueue<String> resultQueue) throws Exception;
    }

    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    // use Sonatype Async Http Client because it supports websockets
    protected AsyncHttpClient httpClient;

    protected final Map<String, BlockingQueue<String>> resultQueues = new HashMap<String, BlockingQueue<String>>();

    public AbstractWebSocketFunctionalTestCase()
    {
        System.setProperty("test.root", ClassUtils.getClassPathRoot(WebSocketFunctionalTestCase.class)
            .getPath());
        setDisposeContextPerClass(true);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        final AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder().setConnectionTimeoutInMs(
            getTestTimeoutSecs() * 1000)
            .setRequestTimeoutInMs(getTestTimeoutSecs() * 1000)
            .build();
        httpClient = new AsyncHttpClient(config);
    }

    @Override
    protected void doTearDown()
    {
        httpClient.close();
    }

    @AfterClass
    public static void disposeContext()
    {
        if (muleContext != null)
        {
            // Mule may shut down polled resources before pollers stop leading to
            // unnecessary errors -> stop pollers beforehand to avoid that
            try
            {
                HttpPollingFunctionalTestCase.stopHttpPollingConnectors(muleContext);
            }
            catch (final MuleException me)
            {
                me.printStackTrace();
            }
        }

        AbstractMuleContextTestCase.disposeContext();
    }

    protected String pollResult(final BlockingQueue<String> resultQueue) throws InterruptedException
    {
        return resultQueue.poll(getTestTimeoutSecs(), TimeUnit.SECONDS);
    }

    protected void sendMessageToWebSocket(final String testMessage,
                                          final String webSocketPath,
                                          final String webSocketProtocol,
                                          final BlockingQueue<String> resultQueue) throws Exception
    {
        httpClient.prepareGet(getWebSocketUrl(webSocketPath))
            .execute(
                new WebSocketUpgradeHandler.Builder().setProtocol(webSocketProtocol)
                    .addWebSocketListener(new DefaultWebSocketListener()
                    {
                        @Override
                        public void onOpen(final WebSocket websocket)
                        {
                            if (testMessage != null)
                            {
                                websocket.sendTextMessage(testMessage);
                            }
                        }

                        @Override
                        public void onMessage(final String message)
                        {
                            resultQueue.offer(message);
                        }

                        @Override
                        public void onError(final Throwable t)
                        {
                            t.printStackTrace();
                        }
                    })
                    .build())
            .get();
    }

    protected BlockingQueue<String> setupOneWayWebSocketFunctionalTestComponent(final String flowName)
        throws Exception
    {
        BlockingQueue<String> resultQueue = resultQueues.get(flowName);

        if (resultQueue == null)
        {
            resultQueue = new LinkedBlockingQueue<String>();
            final BlockingQueue<String> finalResultQueue = resultQueue;
            getFunctionalTestComponent(flowName).setEventCallback(new EventCallback()
            {
                public void eventReceived(final MuleEventContext context, final Object component)
                    throws Exception
                {
                    finalResultQueue.add(context.getMessageAsString());
                }
            });
            resultQueues.put(flowName, resultQueue);
        }
        else
        {
            resultQueue.clear();
        }

        return resultQueue;
    }

    protected String getWebSocketUrl(final String webSocketPath)
    {
        return getBaseWebSocketsUrl() + webSocketPath;
    }

    private String getBaseWebSocketsUrl()
    {
        return "ws://localhost:" + getDynamicPort1() + "/websockets";
    }

    protected Object getDynamicPort1()
    {
        // can't use dynamicPort directly because it returns a different number each
        // time when setDisposeContextPerClass(true)
        return muleContext.getRegistry().get("dynamic-port1");
    }
}

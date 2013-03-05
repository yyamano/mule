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

import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.nio.tcp.notifications.TcpSocketNotification;
import org.mule.transport.nio.tcp.notifications.TcpSocketNotificationListener;
import org.mule.transport.nio.http.notifications.WebSocketNotification;
import org.mule.transport.nio.http.notifications.WebSocketNotificationListener;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@NioTest
public class SocketNotificationsFunctionalTestCase extends AbstractWebSocketFunctionalTestCase
{

    private static CountDownLatch disconnectionLatch;
    private Integer channelId;

    @Override
    protected String getConfigResources()
    {
        return "nio/socket-notifications-functional-conf.xml";
    }

    @BeforeClass
    public static void setUp() {
        disconnectionLatch = new CountDownLatch(1);
    }

    @Test
    public void testWebSocketNotifications() throws Exception
    {
        final CountDownLatch connectionLatch = new CountDownLatch(1);
        final CountDownLatch upgradeLatch = new CountDownLatch(1);

        muleContext.registerListener(new WebSocketNotificationListener<WebSocketNotification>()
        {
            @Override
            public void onNotification(WebSocketNotification notification)
            {
                if(notification.getAction() == WebSocketNotification.UPGRADE)
                {
                    assertEquals(channelId, notification.getChannelId());
                    upgradeLatch.countDown();
                }
            }
        });

        muleContext.registerListener(new TcpSocketNotificationListener<TcpSocketNotification>()
        {
            @Override
            public void onNotification(TcpSocketNotification notification)
            {
                switch (notification.getAction()) {
                    case TcpSocketNotification.CONNECTION:
                        assertNotNull(notification.getChannelId());
                        channelId = notification.getChannelId();
                        connectionLatch.countDown();
                        break;
                    case TcpSocketNotification.DISCONNECTION:
                        assertEquals(channelId, notification.getChannelId());
                        disconnectionLatch.countDown();
                        break;
                    default:
                        fail("Action " + notification.getAction() + " is not a valid TcpSocketNotification action.");
                }
            }
        });
        muleContext.getNotificationManager().addInterfaceToType(WebSocketNotificationListener.class, WebSocketNotification.class);
        muleContext.getNotificationManager().addInterfaceToType(TcpSocketNotificationListener.class, TcpSocketNotification.class);

        testOneWayWebSocket();

        assertTrue(connectionLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
        assertTrue(upgradeLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        assertTrue(disconnectionLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS));
    }

    private void testOneWayWebSocket() throws Exception
    {
        setupOneWayWebSocketFunctionalTestComponent("ws-inbound-ow");
        final String testMessage = RandomStringUtils.randomAlphanumeric(10);
        sendMessageToWebSocket(testMessage, "/ow");
    }

    private BlockingQueue<String> sendMessageToWebSocket(final String testMessage,
                                                         final String webSocketPath) throws Exception
    {
        final BlockingQueue<String> resultQueue = new LinkedBlockingQueue<String>();
        sendMessageToWebSocket(testMessage, webSocketPath, null, resultQueue);
        return resultQueue;
    }

}

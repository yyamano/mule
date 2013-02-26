/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.issues;

import static org.junit.Assert.assertEquals;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class KeepSendSocketOpenMule1491TestCase extends AbstractServiceAndFlowTestCase
{
    private static final String SERVER_RESPONSE = "ok";

    protected static String TEST_TCP_MESSAGE = "Test TCP Request";

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    public KeepSendSocketOpenMule1491TestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "tcp-keep-send-socket-open-service.xml"},
            {ConfigVariant.FLOW, "tcp-keep-send-socket-open-flow.xml"}});
    }

    @Test
    public void testSend() throws Exception
    {
        final MuleClient client = new MuleClient(muleContext);

        for (int i = 0; i < 10; i++)
        {
            final Map<String, Object> props = new HashMap<String, Object>();
            final MuleMessage result = client.send("clientEndpoint", TEST_TCP_MESSAGE + i, props);
            assertEquals(TEST_TCP_MESSAGE + i + " Received", result.getPayloadAsString());
        }
    }

    @Test
    public void testOpen() throws Exception
    {
        useServer("tcp://localhost:" + dynamicPort2.getNumber() + "?connector=openConnectorLength",
            dynamicPort2.getNumber(), 1);
    }

    @Test
    public void testClose() throws Exception
    {
        useServer("tcp://localhost:" + dynamicPort3.getNumber() + "?connector=closeConnectorLength",
            dynamicPort3.getNumber(), 2);
    }

    private void useServer(final String endpoint, final int port, final int count) throws Exception
    {
        final SimpleServerSocket server = new SimpleServerSocket(port);
        try
        {
            new Thread(server).start();
            final MuleClient client = new MuleClient(muleContext);
            assertEquals(SERVER_RESPONSE, client.send(endpoint, "Hello", null).getPayloadAsString());
            logger.debug("got first valid response");
            assertEquals(SERVER_RESPONSE, client.send(endpoint, "world", null).getPayloadAsString());
            logger.debug("got second valid response");
            assertEquals(count, server.getCount());
        }
        finally
        {
            server.close();
        }
    }

    @SuppressWarnings("synthetic-access")
    private class SimpleServerSocket implements Runnable
    {
        private final ServerSocket server;
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger count = new AtomicInteger(0);

        public SimpleServerSocket(final int port) throws Exception
        {
            server = new ServerSocket();
            logger.debug("starting server");
            server.bind(new InetSocketAddress("localhost", port), 3);
        }

        public int getCount()
        {
            return count.get();
        }

        @Override
        public void run()
        {
            try
            {
                while (running.get())
                {
                    final Socket socket = server.accept();
                    logger.debug("have connection " + count);
                    count.incrementAndGet();
                    final InputStream is = new BufferedInputStream(socket.getInputStream());
                    final DataInputStream dis = new DataInputStream(is);
                    final OutputStream os = socket.getOutputStream();
                    final DataOutputStream dos = new DataOutputStream(os);
                    // repeat for as many messages as we receive until null received
                    while (running.get())
                    {
                        int length = 0;

                        try
                        {
                            length = dis.readInt();
                        }
                        catch (final EOFException eofe)
                        {
                            // expected
                        }
                        finally
                        {
                            if (length == 0) break;
                        }

                        logger.debug("will read bytes: " + length);
                        final byte[] buffer = new byte[length];
                        dis.read(buffer);
                        final String msg = new String(buffer);
                        logger.debug("read: " + msg);
                        logger.debug("writing reply");
                        dos.writeInt(SERVER_RESPONSE.getBytes().length);
                        dos.writeBytes(SERVER_RESPONSE);
                        dos.flush();
                    }
                }
            }
            catch (final Exception e)
            {
                // an exception is expected during shutdown
                if (running.get())
                {
                    throw new RuntimeException(e);
                }
            }
            logger.debug("server loop exited");
        }

        public void close()
        {
            try
            {
                running.set(false);
                server.close();
            }
            catch (final Exception e)
            {
                // no-op
            }
            logger.debug("server stopped");
        }
    }
}


package org.mule.transport.tcp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.transport.ReceiveException;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.protocols.SafeProtocol;

public class TcpMessageRequesterTestCase extends AbstractMuleContextTestCase
{
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Before
    public void setUp() throws MuleException
    {
        muleContext.start();
    }

    @After
    public void tearDown() throws MuleException
    {
        muleContext.dispose();
    }

    @Test
    public void testSuccesfullRequestWaitForEver() throws Exception
    {
        doTestSuccesfullRequest(getWaitForeverTimeoutValue());
    }
    
    protected int getWaitForeverTimeoutValue()
    {
        return Integer.MAX_VALUE;
    }

    @Test
    public void testSuccesfullRequestWaitTenSeconds() throws Exception
    {
        doTestSuccesfullRequest(10000);
    }

    private void doTestSuccesfullRequest(final int timeout) throws Exception
    {
        final String testPayload = "w00t!";

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new SafeProtocol().write(baos, getTestEvent(testPayload).getMessage());
        final byte[] serverMessage = baos.toByteArray();

        final ServerSocket welcomeSocket = new ServerSocket(dynamicPort1.getNumber());
        final Runnable testServer = new Runnable()
        {
            public void run()
            {
                try
                {
                    final Socket connectionSocket = welcomeSocket.accept();
                    final OutputStream os = connectionSocket.getOutputStream();
                    os.write(serverMessage);
                    os.flush();
                    connectionSocket.close();
                }
                catch (final IOException ioe)
                {
                    ioe.printStackTrace();
                }
            }
        };
        new Thread(testServer).start();

        final MuleMessage response = new MuleClient(muleContext).request(
            "tcp://localhost:" + dynamicPort1.getNumber(), timeout);
        welcomeSocket.close();

        assertThat(response.getPayloadAsString(), is(testPayload));
    }

    @Test(expected = ReceiveException.class)
    public void testFailedRequest() throws Exception
    {
        new MuleClient(muleContext).request("tcp://localhost:" + dynamicPort2.getNumber(), 1000);
    }
}

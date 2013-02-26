/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.protocols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.tcp.protocols.SlowInputStream;

/**
 * Test by reading characters from a fixed StringBuffer instead of a TCP port.
 */
@SmallTest
@NioTest
public class XmlMessageProtocolTestCase extends AbstractMuleTestCase
{
    private XmlMessageProtocol xmp;

    protected void setProtocol(final XmlMessageProtocol xmp)
    {
        this.xmp = xmp;
    }

    protected byte[] read(final InputStream is) throws IOException
    {
        return (byte[]) xmp.read(is);
    }

    @Before
    public void doSetUp()
    {
        setProtocol(new XmlMessageProtocol());
    }

    protected void doTearDown() throws Exception
    {
        xmp = null;
    }

    @Test
    public void testSingleMessage() throws Exception
    {
        final String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        final ByteArrayInputStream bais = new ByteArrayInputStream(msgData.getBytes());

        final byte[] result = read(bais);
        assertNotNull(result);
        assertEquals(msgData, new String(result));

        assertNull(read(bais));
    }

    @Test
    public void testTwoMessages() throws Exception
    {
        final String[] msgData = {"<?xml version=\"1.0\"?><data>hello</data>",
            "<?xml version=\"1.0\"?><data>goodbye</data>"};

        final ByteArrayInputStream bais = new ByteArrayInputStream((msgData[0] + msgData[1]).getBytes());

        byte[] result = read(bais);
        assertNotNull(result);
        assertEquals(msgData[0], new String(result));

        result = read(bais);
        assertNotNull(result);
        assertEquals(msgData[1], new String(result));

        assertNull(read(bais));
    }

    @Test
    public void testMultipleMessages() throws Exception
    {
        final String[] msgData = {"<?xml version=\"1.0\"?><data>1</data>",
            "<?xml version=\"1.0\"?><data>22</data>", "<?xml version=\"1.0\"?><data>333</data>",
            "<?xml version=\"1.0\"?><data>4444</data>",
            "<?xml version=\"1.0\"?><data>55555</data>",
            "<?xml version=\"1.0\"?><data>666666</data>",
            "<?xml version=\"1.0\"?><data>7777777</data>",
            "<?xml version=\"1.0\"?><data>88888888</data>",
            "<?xml version=\"1.0\"?><data>999999999</data>",
            "<?xml version=\"1.0\"?><data>aaaaaaaaaa</data>",
            "<?xml version=\"1.0\"?><data>bbbbbbbbbbb</data>",
            "<?xml version=\"1.0\"?><data>cccccccccccc</data>",
            "<?xml version=\"1.0\"?><data>ddddddddddddd</data>",
            "<?xml version=\"1.0\"?><data>eeeeeeeeeeeeee</data>",
            "<?xml version=\"1.0\"?><data>fffffffffffffff</data>"};

        final StringBuffer allMsgData = new StringBuffer();

        for (int i = 0; i < msgData.length; i++)
        {
            allMsgData.append(msgData[i]);
        }

        final ByteArrayInputStream bais = new ByteArrayInputStream(allMsgData.toString().getBytes());

        byte[] result;

        for (int i = 0; i < msgData.length; i++)
        {
            result = read(bais);
            assertNotNull(result);
            assertEquals(msgData[i], new String(result));
        }

        assertNull(read(bais));
    }

    @Test
    public void testSlowStream() throws Exception
    {
        final String msgData = "<?xml version=\"1.0\"?><data>hello</data>";

        final SlowInputStream bais = new SlowInputStream(msgData.getBytes());

        final byte[] result = read(bais);
        assertNotNull(result);
        // gets all data because SlowInputStream continues to make more immediately available after each internal read. 
        assertEquals(msgData, new String(result));
    }
}

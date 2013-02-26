/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.integration;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BigInputStream extends InputStream
{

    private static final int SUMMARY_SIZE = 4;
    private static final MessageFormat FORMAT = new MessageFormat(
        "Sent {0,number,#} bytes, {1,number,###.##}% (free {2,number,#}/{3,number,#})");
    private final Log logger = LogFactory.getLog(getClass());
    private final long size;
    private final int messages;

    private long sent = 0;
    private final byte[] data;
    private int dataIndex = 0;
    private long printedMessages = 0;
    private long nextMessage = 0;

    /**
     * @param size Number of bytes to transfer
     * @param messages Number of mesagges logged as INFO
     */
    public BigInputStream(final long size, final int messages)
    {
        this.size = size;
        this.messages = messages;
        data = ("This message is repeated for " + size + " bytes. ").getBytes();
    }

    /**
     * @return String matching
     *         {@link org.mule.tck.functional.FunctionalStreamingTestComponent}
     */
    public String summary()
    {

        final byte[] tail = new byte[SUMMARY_SIZE];
        for (int i = 0; i < SUMMARY_SIZE; ++i)
        {
            tail[i] = data[(int) ((sent - SUMMARY_SIZE + i) % data.length)];
        }
        return "Received stream; length: " + sent + "; '" + new String(data, 0, 4) + "..." + new String(tail)
               + "'";
    }

    @Override
    public int read() throws IOException
    {
        if (sent == size)
        {
            return -1;
        }
        else
        {
            if (++sent > nextMessage)
            {
                final double percent = 100l * sent / ((double) size);
                final Runtime runtime = Runtime.getRuntime();
                logger.info(FORMAT.format(new Object[]{Long.valueOf(sent), Double.valueOf(percent),
                    Long.valueOf(runtime.freeMemory()), Long.valueOf(runtime.maxMemory())}));
                nextMessage = ++printedMessages * ((int) Math.floor(((double) size) / (messages - 1)) - 1);
            }
            if (dataIndex == data.length)
            {
                dataIndex = 0;
            }
            return data[dataIndex++];
        }
    }

    @Override
    public int available() throws IOException
    {
        return (int) Math.min(size - sent, Integer.MAX_VALUE);
    }

}

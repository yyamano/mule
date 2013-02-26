/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalStreamingTestComponent;
import org.mule.transport.tcp.integration.BigInputStream;

/**
 * IMPORTANT - DO NOT RUN THIS TEST IN AN IDE WITH LOG LEVEL OF DEBUG. USE INFO TO
 * SEE DIAGNOSTICS. OTHERWISE THE CONSOLE OUTPUT WILL BE SIMILAR SIZE TO DATA
 * TRANSFERRED, CAUSING CONFUSNG AND PROBABLY FATAL MEMORY USE.
 */
public abstract class AbstractStreamingCapacityTestCase extends AbstractServiceAndFlowTestCase
{
    public static final long ONE_KB = 1024;
    public static final long ONE_MB = ONE_KB * ONE_KB;
    public static final long ONE_GB = ONE_KB * ONE_MB;
    public static final int MESSAGES = 21;

    private final long size;

    public AbstractStreamingCapacityTestCase(final ConfigVariant variant,
                                             final String configResources,
                                             final long size)
    {
        super(variant, configResources);
        this.size = size;
    }

    @Test
    public void testSend() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<String>();

        final EventCallback callback = new EventCallback()
        {
            @Override
            public synchronized void eventReceived(final MuleEventContext context, final Object component)
            {
                try
                {
                    final FunctionalStreamingTestComponent ftc = (FunctionalStreamingTestComponent) component;
                    message.set(ftc.getSummary());
                    latch.countDown();
                }
                catch (final Exception e)
                {
                    logger.error(e.getMessage(), e);
                }
            }
        };

        final Object ftc = getComponent("testComponent");
        assertTrue("FunctionalStreamingTestComponent expected",
            ftc instanceof FunctionalStreamingTestComponent);
        assertNotNull(ftc);
        ((FunctionalStreamingTestComponent) ftc).setEventCallback(callback, size);

        final Runtime runtime = Runtime.getRuntime();
        runtime.gc(); // i know, i know...
        final long freeStart = runtime.freeMemory();
        final long maxStart = runtime.maxMemory();
        final long timeStart = System.currentTimeMillis();

        final BigInputStream stream = new BigInputStream(size, MESSAGES);
        final MuleClient client = new MuleClient(muleContext);
        // dynamically get the endpoint to send to
        client.dispatch(
            ((InboundEndpoint) client.getMuleContext().getRegistry().lookupObject("testInbound")).getAddress(),
            new DefaultMuleMessage(stream, muleContext));

        // if we assume 1MB/sec then we need at least...
        final long pause = Math.max(size / ONE_MB, 60 * 10) + 10;
        logger.info("Waiting for up to " + pause + " seconds");

        latch.await(pause, TimeUnit.SECONDS);
        final long timeEnd = System.currentTimeMillis();

        assertEquals(stream.summary(), message.get());

        // neither of these memory tests are really reliable, but if we stay with 1.4
        // i don't know of anything better. if these fail in practice i guess we just
        // remove them.
        long freeEnd = -1;
        for (int i = 0; i < 100; i++)
        {
            runtime.gc();
            Thread.sleep(100L);
            final long newFreeEnd = runtime.freeMemory();
            if (newFreeEnd <= freeEnd)
            {
                break;
            }
            freeEnd = newFreeEnd;
        }

        final long delta = freeStart - freeEnd;
        final double speed = size / (double) (timeEnd - timeStart) * 1000 / ONE_MB;
        logger.info("Transfer speed " + speed + " MB/s (" + size + " B in " + (timeEnd - timeStart) + " ms)");

        final double expectPercent = 20d; // add a little more wiggle room than 10%
        final double usePercent = 100.0 * delta / size;
        logger.info("Memory delta " + delta + " B = " + usePercent + "%");

        final String assertMessage = String.format(
            "Expected memory usage to be lower than %f%% but was %f%%", Double.valueOf(expectPercent),
            Double.valueOf(usePercent));
        assertTrue(assertMessage, usePercent < expectPercent);

        final long maxEnd = runtime.maxMemory();
        assertEquals("Max memory shifted", 0, maxEnd - maxStart);
    }
}

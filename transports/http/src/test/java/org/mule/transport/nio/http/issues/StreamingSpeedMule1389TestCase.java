/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.issues;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.nio.tcp.integration.AbstractStreamingCapacityTestCase;
import org.mule.util.SystemUtils;

@NioTest
public class StreamingSpeedMule1389TestCase extends AbstractStreamingCapacityTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    public StreamingSpeedMule1389TestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources, 100 * ONE_MB);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "streaming-speed-mule-1389-service.xml"},
            {ConfigVariant.FLOW, "streaming-speed-mule-1389-flow.xml"}});
    }

    @Override
    protected boolean isDisabledInThisEnvironment()
    {
        // MULE-4713
        return (SystemUtils.isIbmJDK() && org.apache.commons.lang.SystemUtils.isJavaVersionAtLeast(160));
    }
}

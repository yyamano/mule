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

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.transport.nio.tcp.TcpConnector;
import org.mule.transport.tcp.TcpFunctionalTestCase;

/**
 * This is just to check that the Boolean (rather than boolean) doesn't cause any
 * problems
 */
@NioTest
public class ReuseMule2069TestCase extends TcpFunctionalTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    public ReuseMule2069TestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "nio/reuse-mule-2069-service.xml"},
            {ConfigVariant.FLOW, "nio/reuse-mule-2069-flow.xml"}});
    }

    @Test
    public void testReuseSetOnConnector()
    {
        assertTrue(((TcpConnector) muleContext.getRegistry().lookupConnector("tcpConnector")).isReuseAddress());
    }
}

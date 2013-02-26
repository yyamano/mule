/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.InboundEndpoint;

import java.util.HashMap;
import java.util.Map;

import org.junit.Rule;

@NioTest
public class TcpConnectorFactoryTestCase extends org.mule.transport.tcp.TcpConnectorFactoryTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    protected void assertConnector(InboundEndpoint endpoint)
    {
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof TcpConnector);
    }
    
    @Override
    protected Map<String, String> getUriMap()
    {
        Map<String, String> map = super.getUriMap();
        Map<String, String> rewrittenMap = new HashMap<String, String>(map.size());
        for (String inputUri : map.keySet())
        {
            rewrittenMap.put(inputUri, map.get(inputUri).replaceFirst("tcp", "niotcp"));
        }
        return rewrittenMap;
    }
}

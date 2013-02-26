/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@LegacyIoTest
public class TcpConnectorFactoryTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void createFromFactory() throws Exception
    {
        Map<String, String> inputUriToOutputUri = getUriMap();
        for (String inputUri : inputUriToOutputUri.keySet())
        {
            final InboundEndpoint endpoint = muleContext.getEndpointFactory()
                .getInboundEndpoint(inputUri);
            assertNotNull(endpoint);
            assertConnector(endpoint);
            String expectedOutputUri = inputUriToOutputUri.get(inputUri);
            assertEquals(expectedOutputUri, endpoint.getEndpointURI().getAddress());
        }
    }
    
    protected void assertConnector(InboundEndpoint endpoint)
    {
        assertNotNull(endpoint.getConnector());
        assertTrue(endpoint.getConnector() instanceof TcpConnector);
    }
    
    protected Map<String, String> getUriMap()
    {
        Map<String, String> uris = new HashMap<String, String>();
        uris.put("tcp://7877", "tcp://localhost:7877");
        uris.put("tcp://localhost:123", "tcp://localhost:123");
        return uris;
    }
}

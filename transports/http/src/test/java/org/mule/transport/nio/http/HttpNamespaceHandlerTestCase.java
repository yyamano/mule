/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.routing.filter.Filter;
import org.mule.transport.nio.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.nio.http.transformers.HttpResponseToObject;
import org.mule.transport.nio.http.transformers.HttpResponseToString;
import org.mule.transport.nio.http.transformers.ObjectToHttpRequest;
import org.mule.transport.nio.http.transformers.ObjectToHttpResponse;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;

@NioTest
public class HttpNamespaceHandlerTestCase extends AbstractNamespaceHandlerTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);

    public HttpNamespaceHandlerTestCase()
    {
        super("http");
    }

    @Test
    public void testConnectorProperties()
    {
        final HttpConnector connector = (HttpConnector) muleContext.getRegistry().lookupConnector(
            "httpConnector");
        testBasicProperties(connector);
    }

    @Test
    public void testPollingProperties()
    {
        final HttpPollingConnector connector = (HttpPollingConnector) muleContext.getRegistry()
            .lookupConnector("polling");
        assertNotNull(connector);
        assertEquals(3456, connector.getPollingFrequency());
        assertFalse(connector.isCheckEtag());
        assertFalse(connector.isDiscardEmptyContent());
    }

    @Test
    public void testTransformersOnEndpoints() throws Exception
    {
        final Object transformer1 = lookupInboundEndpoint("ep1").getMessageProcessors().get(0);
        assertNotNull(transformer1);
        assertEquals(HttpResponseToObject.class, transformer1.getClass());

        final Object transformer2 = lookupInboundEndpoint("ep2").getMessageProcessors().get(0);
        assertNotNull(transformer2);
        assertEquals(HttpResponseToString.class, transformer2.getClass());

        final Object transformer3 = lookupInboundEndpoint("ep3").getMessageProcessors().get(0);
        assertNotNull(transformer3);
        assertEquals(ObjectToHttpResponse.class, transformer3.getClass());

        final Object transformer4 = lookupInboundEndpoint("ep4").getMessageProcessors().get(0);
        assertNotNull(transformer4);
        assertEquals(ObjectToHttpRequest.class, transformer4.getClass());

        final Object transformer6 = lookupInboundEndpoint("ep6").getMessageProcessors().get(0);
        assertNotNull(transformer6);
        assertEquals(ObjectToHttpResponse.class, transformer6.getClass());
    }

    @Test
    public void testFiltersOnEndpoints() throws Exception
    {
        final Filter filter = lookupInboundEndpoint("ep5").getFilter();
        assertNotNull(filter);
        assertEquals(HttpRequestWildcardFilter.class, filter.getClass());
        final HttpRequestWildcardFilter requestWildcardFilter = (HttpRequestWildcardFilter) filter;
        assertEquals("foo*", requestWildcardFilter.getPattern());
    }

    private InboundEndpoint lookupInboundEndpoint(final String endpointName) throws Exception
    {
        final InboundEndpoint endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointName);
        assertNotNull(endpoint);
        return endpoint;
    }
}

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
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.transport.MessageReceiver;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.AbstractMessageReceiverTestCase;
import org.mule.transport.nio.http.transformers.ObjectToHttpResponse;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.util.CollectionUtils;

@NioTest
public class HttpMessageReceiverTestCase extends AbstractMessageReceiverTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    private static final String CONTEXT_PATH = "/resources";
    private static final String CLIENT_PATH = "/resources/client";
    private static final String CLIENT_NAME_PATH = "/resources/client/name";

    private HttpMessageReceiver httpMessageReceiver;

    @Before
    public void setUp() throws Exception
    {
        httpMessageReceiver = (HttpMessageReceiver) getMessageReceiver();
    }

    @Override
    public MessageReceiver getMessageReceiver() throws Exception
    {
        Service mockService = mock(Service.class);

        return new HttpMessageReceiver(endpoint.getConnector(), mockService, endpoint);
    }

    @SuppressWarnings("unchecked")
    @Override
    public InboundEndpoint getEndpoint() throws Exception
    {
        final EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(HttpConnector.HTTP + "://localhost:6789",
            muleContext);
        endpointBuilder.setResponseMessageProcessors(CollectionUtils.singletonList(new ObjectToHttpResponse()));
        endpoint = muleContext.getEndpointFactory().getInboundEndpoint(endpointBuilder);
        return endpoint;
    }

    @Test
    public void testProcessResourceRelativePath()
    {
        assertEquals("client", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_PATH));
    }

    @Test
    public void testProcessRelativePathSameLevel()
    {
        assertEquals("", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CONTEXT_PATH));
    }

    @Test
    public void testProcessResourcePropertyRelativePath()
    {
        assertEquals("client/name", httpMessageReceiver.processRelativePath(CONTEXT_PATH, CLIENT_NAME_PATH));
    }
}

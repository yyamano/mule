/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.util.StringUtils;

@NioTest
public class HttpBadEncodingFunctionalTestCase extends HttpEncodingFunctionalTestCase
{
    public HttpBadEncodingFunctionalTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void testSend() throws Exception
    {
        // Send as bytes so that the StringRequestEntity isn't used. If it is used
        // it will throw an exception and stop us from testing the server side.
        final URI uri = ((EndpointURIEndpointBuilder) muleContext.getRegistry()
            .lookupObject("clientEndpoint")).getEndpointBuilder().getEndpoint().getUri();
        final PostMethod postMethod = new PostMethod(uri.toString().replace(HttpConnector.HTTP, "http"));
        postMethod.setRequestEntity(new ByteArrayRequestEntity(TEST_MESSAGE.getBytes(),
            "text/plain; charset=UTFF-912"));
        final int statusCode = new HttpClient().executeMethod(postMethod);

        assertEquals(500, statusCode);
        assertTrue(StringUtils.containsIgnoreCase(postMethod.getResponseBodyAsString(), "exception"));
    }
}

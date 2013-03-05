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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;

public class HttpOutboundHeadersPropagationTestCase extends HttpFunctionalTestCase
{
    protected static String TEST_MESSAGE = "Test Http Request (R�dgr�d), 57 = \u06f7\u06f5 in Arabic";

    public HttpOutboundHeadersPropagationTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.FLOW, "nio/http-outbound-headers-propagation-flow.xml"}});
    }

    @Ignore
    @Override
    public void testSend() throws Exception
    {
        // NOOP
    }

    @Test
    public void testOutboundHttpContentType() throws Exception
    {
        final MuleClient client = muleContext.getClient();
        final Map<String, Object> msgProps = new HashMap<String, Object>();
        msgProps.put("custom-header", "value-custom-header");
        client.dispatch("vm://in", "HelloWorld!", msgProps);

        final MuleMessage reply = client.request("vm://out", getTestTimeoutSecs() * 1000);

        @SuppressWarnings("unchecked")
        final Map<String, Object> headers = (Map<String, Object>) reply.getPayload();

        for (final String header : HttpConstants.REQUEST_HEADER_NAMES.values())
        {
            if (!HttpConstants.HEADER_EXPECT.equals(header))
            {
                if (HttpConstants.HEADER_COOKIE.equals(header))
                {
                    assertNotNull("Request header <" + header + "> should be defined.",
                        headers.get(HttpConnector.HTTP_COOKIES_PROPERTY));
                }
                else
                {
                    assertNotNull("Request header <" + header + "> should be defined.", headers.get(header));
                }
            }

        }
        for (final String header : HttpConstants.GENERAL_AND_ENTITY_HEADER_NAMES.values())
        {
            assertNotNull("General or Entity header <" + header + "> should be defined.", headers.get(header));
        }
        for (final String header : HttpConstants.RESPONSE_HEADER_NAMES.values())
        {
            assertNull("Response header <" + header + "> should not be defined.", headers.get(header));
        }
        assertNotNull(reply);
    }
}

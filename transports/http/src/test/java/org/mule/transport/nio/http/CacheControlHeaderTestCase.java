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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

@SmallTest
public class CacheControlHeaderTestCase extends AbstractMuleTestCase
{
    private static final String HEADER_DIRECTIVE = "#[header:directive]";
    private static final String HEADER_MAX_AGE = "#[header:maxAge]";
    private static final String HEADER_MUST_REVALIDATE = "#[header:mustRevalidate]";
    private static final String HEADER_NO_CACHE = "#[header:noCache]";
    private static final String HEADER_NO_STORE = "#[header:noStore]";

    private MuleEvent mockMuleEvent;
    private ExpressionManager mockExpressionManager;

    @Before
    public void setUp()
    {
        mockMuleEvent = mock(MuleEvent.class);
        final MuleContext muleContext = mock(MuleContext.class);
        when(mockMuleEvent.getMuleContext()).thenReturn(muleContext);
        mockExpressionManager = mock(ExpressionManager.class);
        when(muleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test
    public void testCacheControlByDefault()
    {
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.parse(mockMuleEvent);
        assertEquals("", cacheControl.toString());
    }

    @Test
    public void testCacheControlFullConfig()
    {
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("public");
        cacheControl.setMaxAge("3600");
        cacheControl.setMustRevalidate("true");
        cacheControl.setNoCache("true");
        cacheControl.setNoStore("true");
        mockParse();
        cacheControl.parse(mockMuleEvent);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheControlWrongDirective()
    {
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective("anyDirective");
        mockParse();
        cacheControl.parse(mockMuleEvent);
    }

    @Test
    public void testCacheControlWithExpressions()
    {
        final CacheControlHeader cacheControl = new CacheControlHeader();
        cacheControl.setDirective(HEADER_DIRECTIVE);
        cacheControl.setMaxAge(HEADER_MAX_AGE);
        cacheControl.setMustRevalidate(HEADER_MUST_REVALIDATE);
        cacheControl.setNoCache(HEADER_NO_CACHE);
        cacheControl.setNoStore(HEADER_NO_STORE);

        when(mockExpressionManager.parse(HEADER_DIRECTIVE, mockMuleEvent)).thenReturn("public");
        when(mockExpressionManager.parse(HEADER_MAX_AGE, mockMuleEvent)).thenReturn("3600");
        when(mockExpressionManager.parse(HEADER_MUST_REVALIDATE, mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_CACHE, mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse(HEADER_NO_STORE, mockMuleEvent)).thenReturn("true");

        cacheControl.parse(mockMuleEvent);
        assertEquals("public,no-cache,no-store,must-revalidate,max-age=3600", cacheControl.toString());
    }

    private void mockParse()
    {
        when(mockExpressionManager.parse(anyString(), Mockito.any(MuleEvent.class))).thenAnswer(
            new Answer<Object>()
            {
                @Override
                public Object answer(final InvocationOnMock invocation) throws Throwable
                {
                    return invocation.getArguments()[0];
                }
            });
    }
}

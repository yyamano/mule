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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.jboss.netty.handler.codec.http.Cookie;
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
public class CookieWrapperTestCase extends AbstractMuleTestCase
{
    private CookieWrapper cookieWrapper;
    private ExpressionManager mockExpressionManager;
    private MuleEvent mockMuleEvent;

    @Before
    public void setUp()
    {
        cookieWrapper = new CookieWrapper();
        mockMuleEvent = mock(MuleEvent.class);
        final MuleContext muleContext = mock(MuleContext.class);
        when(mockMuleEvent.getMuleContext()).thenReturn(muleContext);
        mockExpressionManager = mock(ExpressionManager.class);
        when(muleContext.getExpressionManager()).thenReturn(mockExpressionManager);
    }

    @Test
    public void testCookieWrapper() throws ParseException
    {
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setDomain("localhost");
        cookieWrapper.setPath("/");
        cookieWrapper.setMaxAge("3600");
        cookieWrapper.setSecure("true");
        cookieWrapper.setVersion("1");

        mockParse();

        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        assertEquals("test", cookie.getName());
        assertEquals("test", cookie.getValue());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isSecure());
        assertEquals(1, cookie.getVersion());
    }

    @Test
    public void testCookieWrapperWithExpressions() throws ParseException
    {
        cookieWrapper.setName("#[name]");
        cookieWrapper.setValue("#[value]");
        cookieWrapper.setDomain("#[domain]");
        cookieWrapper.setPath("#[path]");
        cookieWrapper.setMaxAge("#[maxAge]");
        cookieWrapper.setSecure("#[secure]");
        cookieWrapper.setVersion("#[version]");

        when(mockExpressionManager.parse("#[name]", mockMuleEvent)).thenReturn("test");
        when(mockExpressionManager.parse("#[value]", mockMuleEvent)).thenReturn("test");
        when(mockExpressionManager.parse("#[domain]", mockMuleEvent)).thenReturn("localhost");
        when(mockExpressionManager.parse("#[path]", mockMuleEvent)).thenReturn("/");
        when(mockExpressionManager.parse("#[maxAge]", mockMuleEvent)).thenReturn("3600");
        when(mockExpressionManager.parse("#[secure]", mockMuleEvent)).thenReturn("true");
        when(mockExpressionManager.parse("#[version]", mockMuleEvent)).thenReturn("1");

        acceptMockExpressions("#[name]", "#[value]", "#[domain]", "#[path]", "#[maxAge]", "#[secure]",
            "#[version]");

        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        assertEquals("test", cookie.getName());
        assertEquals("test", cookie.getValue());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.isSecure());
        assertEquals(1, cookie.getVersion());
    }

    @Test
    public void testCookieWrapperOnlyRequiredAttributes() throws ParseException
    {
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");

        mockParse();

        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        assertEquals("test=test", cookie.toString());
    }

    @Test
    public void testCookieWrapperStringExpiryDate() throws ParseException
    {
        final Calendar expireAt = new GregorianCalendar();
        expireAt.add(GregorianCalendar.DAY_OF_WEEK, 1);

        final SimpleDateFormat formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String expiryDate = formatter.format(expireAt.getTime());

        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate(expiryDate);

        mockParse();
        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        // give some leeway to the test
        assertTrue(cookie.getMaxAge() >= 86400 - getTestTimeoutSecs());
    }

    @Test
    public void testCookieWrapperExpiryDate() throws ParseException
    {
        final Date now = new Date();
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate(now);

        mockParse();
        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    public void testCookieWrapperExpiryDateExpression() throws ParseException
    {
        final Date now = new Date();
        cookieWrapper.setName("test");
        cookieWrapper.setValue("test");
        cookieWrapper.setExpiryDate("#[expiryDate]");

        when(mockExpressionManager.isExpression("#[expiryDate]")).thenReturn(true);
        when(mockExpressionManager.evaluate("#[expiryDate]", mockMuleEvent)).thenReturn(now);
        mockParse();

        cookieWrapper.parse(mockMuleEvent);
        final Cookie cookie = cookieWrapper.createCookie(1);

        assertEquals(0, cookie.getMaxAge());
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

    private void acceptMockExpressions(final String... expressions)
    {
        for (final String expression : expressions)
        {
            when(mockExpressionManager.isExpression(expression)).thenReturn(true);
        }
    }
}

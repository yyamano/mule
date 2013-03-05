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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class CookieHelperTestCase extends AbstractMuleTestCase
{
    private static final String COOKIE_1_NAME = "cookie1";
    private static final String COOKIE_1_ORIGINAL_VALUE = "value1";
    private static final String COOKIE_2_NAME = "cookie2";
    private static final String COOKIE_2_VALUE = "value2";
    private static final String COOKIE_1_NEW_VALUE = "newValue1 That Overrides Previous One";

    @Test
    @SuppressWarnings("unchecked")
    public void testPutAndMergeCookieObjectMapOfStringString_CookiesInMap_NewCookiesInMap()
    {
        Map<String, String> cookiesObject = new HashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);

        assertEquals(1, cookiesObject.size());

        final Map<String, String> newCookiesMap = new HashMap<String, String>();
        newCookiesMap.put(COOKIE_1_NAME, COOKIE_1_NEW_VALUE);
        newCookiesMap.put(COOKIE_2_NAME, COOKIE_2_VALUE);
        cookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesMap);

        assertEquals(2, cookiesObject.size());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject.get(COOKIE_1_NAME));
        assertEquals(COOKIE_2_VALUE, cookiesObject.get(COOKIE_2_NAME));

        final Map<String, String> unModifiedCookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(
            cookiesObject, (Map<String, String>) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.size());
    }

    @Test
    public void testPutAndMergeCookieObjectMapOfStringString_CookiesInArray_NewCookiesInMap()
    {
        Cookie[] cookiesObject = new Cookie[]{new DefaultCookie(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE)};

        final Map<String, String> newCookiesMap = new HashMap<String, String>();
        newCookiesMap.put(COOKIE_1_NAME, COOKIE_1_NEW_VALUE);
        newCookiesMap.put(COOKIE_2_NAME, COOKIE_2_VALUE);

        cookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesMap);

        assertEquals(2, cookiesObject.length);

        assertEquals(COOKIE_1_NAME, cookiesObject[0].getName());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesObject[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesObject[1].getValue());

        final Cookie[] unModifiedCookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject,
            (Map<String, String>) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.length);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutAndMergeCookieObjectCookieArray_CookiesInMap_NewCookiesInArray()
    {
        Map<String, String> cookiesObject = new HashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);

        assertEquals(1, cookiesObject.size());

        final Cookie[] newCookiesArray = new Cookie[]{new DefaultCookie(COOKIE_1_NAME, COOKIE_1_NEW_VALUE),
            new DefaultCookie(COOKIE_2_NAME, COOKIE_2_VALUE)};

        cookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesArray);

        assertEquals(2, cookiesObject.size());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject.get(COOKIE_1_NAME));
        assertEquals(COOKIE_2_VALUE, cookiesObject.get(COOKIE_2_NAME));

        final Map<String, String> unModifiedCookiesObject = (Map<String, String>) CookieHelper.putAndMergeCookie(
            cookiesObject, (Cookie[]) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.size());
    }

    @Test
    public void testPutAndMergeCookieObjectCookieArray_CookiesInArray_NewCookiesInArray()
    {
        Cookie[] cookiesObject = new Cookie[]{new DefaultCookie(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE)};

        assertEquals(1, cookiesObject.length);

        final Cookie[] newCookiesArray = new Cookie[]{new DefaultCookie(COOKIE_1_NAME, COOKIE_1_NEW_VALUE),
            new DefaultCookie(COOKIE_2_NAME, COOKIE_2_VALUE)};

        cookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject, newCookiesArray);

        assertEquals(2, cookiesObject.length);

        assertEquals(COOKIE_1_NAME, cookiesObject[0].getName());
        assertEquals(COOKIE_1_NEW_VALUE, cookiesObject[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesObject[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesObject[1].getValue());

        final Cookie[] unModifiedCookiesObject = (Cookie[]) CookieHelper.putAndMergeCookie(cookiesObject,
            (Cookie[]) null);
        assertSame(cookiesObject, unModifiedCookiesObject);
        assertEquals(2, cookiesObject.length);
    }

    @Test
    public void testAsArrayOfCookies_CookiesInArray() throws Exception
    {
        final Cookie[] cookiesObject = new Cookie[]{new DefaultCookie(COOKIE_1_NAME, COOKIE_1_NEW_VALUE)};
        assertSame(cookiesObject, CookieHelper.asArrayOfCookies(cookiesObject));

        final Cookie[] emptyArray = CookieHelper.asArrayOfCookies(null);
        assertNotNull("A null cookiesObject should return a non null array", emptyArray);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testAsArrayOfCookies_CookiesInMap() throws Exception
    {
        final Map<String, String> cookiesObject = new LinkedHashMap<String, String>();
        cookiesObject.put(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);
        cookiesObject.put(COOKIE_2_NAME, COOKIE_2_VALUE);

        final Cookie[] cookiesAsArray = CookieHelper.asArrayOfCookies(cookiesObject);
        assertNotNull("Array of cookies should not be null", cookiesAsArray);

        assertEquals(2, cookiesAsArray.length);

        assertEquals(COOKIE_1_NAME, cookiesAsArray[0].getName());
        assertEquals(COOKIE_1_ORIGINAL_VALUE, cookiesAsArray[0].getValue());

        assertEquals(COOKIE_2_NAME, cookiesAsArray[1].getName());
        assertEquals(COOKIE_2_VALUE, cookiesAsArray[1].getValue());

    }

    @Test
    public void testResolveCookieStorageType() throws Exception
    {
        assertSame(CookieStorageType.MAP_STRING_STRING,
            CookieStorageType.resolveCookieStorageType(new HashMap<String, String>()));

        assertSame(CookieStorageType.ARRAY_OF_COOKIES, CookieStorageType.resolveCookieStorageType(null));

        assertSame(CookieStorageType.ARRAY_OF_COOKIES,
            CookieStorageType.resolveCookieStorageType(new Cookie[2]));

        try
        {
            CookieStorageType.resolveCookieStorageType(new Object());
            fail("It should have thrown an exception since Object it is not a valid type");
        }
        catch (final IllegalArgumentException e)
        {
            assertTrue(e.getMessage().contains("Invalid cookiesObject"));
        }
    }

    @Test
    public void formattingCookie0WithMaxAgeHeaderShouldCreateExpireDate() throws Exception
    {
        final Cookie cookie = new DefaultCookie(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);
        cookie.setMaxAge(1000);

        final String cookieString = CookieHelper.formatCookieForASetCookieHeader(cookie, 0);
        assertTrue(cookieString.contains("Expires="));
    }

    @Test
    public void formattingCookie1WithMaxAgeHeaderShouldPreserveMaxAge() throws Exception
    {
        final Cookie cookie = new DefaultCookie(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);
        cookie.setMaxAge(1000);

        final String cookieString = CookieHelper.formatCookieForASetCookieHeader(cookie, 1);
        assertTrue(!cookieString.contains("Expires="));
        assertTrue(cookieString.contains("Max-Age=1000"));
    }

    @Test
    public void formattingCookieWithoutExpiresHeaderShouldNotHaveExpireDateSet() throws Exception
    {
        final Cookie cookie = new DefaultCookie(COOKIE_1_NAME, COOKIE_1_ORIGINAL_VALUE);
        final String cookieStr = CookieHelper.formatCookieForASetCookieHeader(cookie, 0);

        final Cookie[] cookies = CookieHelper.parseCookiesAsAClient(cookieStr);
        assertEquals(-1, cookies[0].getMaxAge());
    }
}

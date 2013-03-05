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

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.util.ArrayUtils;

/**
 * <p>
 * Helper functions for parsing, formatting, storing and retrieving cookie headers.
 * </p>
 * <p>
 * It is important that all access to Cookie data is done using this class. This will
 * help to prevent ClassCastExceptions and data corruption.
 * </p>
 * <p>
 * The reasons for such a very complex CookieHelper class are historical and are
 * related to the fact that cookies are a multivalued property and we store them as a
 * single message property under the name
 * {@linkplain HttpConnector#HTTP_COOKIES_PROPERTY "cookies"}.
 * </p>
 * <p>
 * In an HTTP message going from client to server the cookies come on their own
 * {@linkplain HttpConstants#HEADER_COOKIE "Cookie"} header. The HTTP message can
 * have several of these Cookie headers and each of them can store 1 or more cookies.
 * One problem with this is that in Mule we use {@link Map} instances to store the
 * HTTP headers and this means that we can only have one object with the key
 * {@linkplain HttpConnector#HTTP_COOKIES_PROPERTY "cookies"} (yes, we use that
 * constant instead of {@linkplain HttpConstants#HEADER_COOKIE "Cookie"} when we
 * store the cookies inside a {@link MuleMessage}).
 * </p>
 * <p>
 * In an HTTP message going from server to client the Cookies go in their own
 * {@linkplain HttpConstants#HEADER_COOKIE_SET "Set-Cookie"} header. But, again,
 * internally we store all the HTTP headers inside a {@link Map} that maps each HTTP
 * header with a single value. For Cookies it is a special case so have to be able to
 * store many cookies in the value from that map.
 * </p>
 * <p>
 * With all these layed out one could say that we could just have a
 * {@link Collection} of {@link Cookie} instances. But this is not that simple. In
 * some parts of the code the cookies are stored as an array of Cookies and in some
 * others it is stored as a {@link Map} where each entry corresponds to a cookie's
 * name/value pair (which is not strictly a cookie). Specifically, when parsing
 * cookies from the client (ie, acting as a server), the code stores it as an array
 * of cookies. When the cookies are specified as a property in the endpoint (like <a
 * href=
 * "http://www.mulesoft.org/documentation/display/MULE3USER/HTTP+Transport#HTTPTransport-Cookies"
 * >explained in the docs</a>), they are stored as a {@link Map}.
 * </p>
 * <p>
 * This class has helper methods that helps making code that is independent of the
 * way the cookies are stored and still keep backward compatibility. It is very
 * hacky, but I think it is better than what we had before.
 * </p>
 * <p>
 * <b>Know Limitation:</b> because of how cookies are handled in Mule, we don't
 * handle well the host, port and path of a Cookie. We just handle Cookies as if they
 * were only name/value pairs. This means that, for example, if a message with
 * cookies is received on an endpoint called http://localhost:4020/hello (1) and that
 * message goes to http://www.mulesoft.org/jira/ (2), then service (2) will receive
 * all the cookies that were sent to service (1) as if they were their own.
 * Furthermore, the same thing will happend on the response: all the returned cookies
 * from service (2) will reach service (1) and then the client will receive them as
 * if they were from service (1).
 * </p>
 */
public abstract class CookieHelper
{
    public static final String EXPIRE_PATTERN = "EEE, d-MMM-yyyy HH:mm:ss z";

    protected static final Cookie[] EMPTY_COOKIE_ARRAY = new Cookie[0];
    protected static final CookieDecoder LENIENT_COOKIE_DECODER = new CookieDecoder(true);

    private CookieHelper()
    {
        throw new UnsupportedOperationException("Do not instantiate");
    }

    /**
     * This method parses the value of {@linkplain HttpConstants#HEADER_COOKIE_SET
     * "Set-Cookie"} HTTP header, returning an array with all the {@link Cookie}s
     * found. This method is intended to be used from the client side of the HTTP
     * connection.
     * 
     * @param cookieHeaderValue the value with the cookie/s to parse.
     */
    public static Cookie[] parseCookiesAsAClient(final String cookieHeaderValue)
    {
        return LENIENT_COOKIE_DECODER.decode(cookieHeaderValue).toArray(EMPTY_COOKIE_ARRAY);

    }

    /**
     * This method parses the value of an HTTP
     * {@linkplain HttpConstants#HEADER_COOKIE "Cookie"} header that comes from a
     * client to a server. It returns all the Cookies present in the header.
     * 
     * @param headerValue the value of the header from which the cookie will be
     *            parsed.
     */
    public static Cookie[] parseCookiesAsAServer(final String headerValue)
    {
        return parseCookiesAsAClient(headerValue);
    }

    /**
     * This method formats the cookie so it can be send from server to client in a
     * {@linkplain HttpConstants#HEADER_COOKIE_SET "Set-Cookie"} header.
     */
    public static String formatCookieForASetCookieHeader(final Cookie cookie)
    {
        final CookieEncoder serverCookieEncoder = new CookieEncoder(true);
        serverCookieEncoder.addCookie(cookie);
        return serverCookieEncoder.encode();
    }

    /**
     * This method formats the cookie so it can be send from server to client in a
     * {@linkplain HttpConstants#HEADER_COOKIE_SET "Set-Cookie"} header.
     */
    public static String formatCookieForASetCookieHeader(final Cookie cookie, final int cookieVersion)
    {
        cookie.setVersion(cookieVersion);
        final CookieEncoder serverCookieEncoder = new CookieEncoder(true);
        serverCookieEncoder.addCookie(cookie);
        return serverCookieEncoder.encode();
    }

    /**
     * Adds to the client all the cookies present in the cookiesObject.
     * 
     * @param cookiesObject this must be either a {@link Map Map&lt;String,
     *            String&gt;} or a {@link Cookie Cookie[]}. It can be null.
     * @param event this one is used only if the cookies are stored in a {@link Map}
     *            in order to resolve expressions with the {@link ExpressionManager}.
     */
    public static void addCookiesToHttpMessage(final HttpMessage httpMessage,
                                               final Object cookiesObject,
                                               final int cookieVersion,
                                               final MuleEvent event)
    {
        CookieStorageType.resolveCookieStorageType(cookiesObject).addCookiesToHttpMessage(httpMessage,
            cookiesObject, cookieVersion, event);
    }

    /**
     * <p>
     * This method merges a new Cookie (or override the previous one if it exists) to
     * the preExistentCookies. The result (the old cookies with the new one added) is
     * returned. If a cookie with the same name already exists, then it will be
     * overridden.
     * </p>
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on Cookie[]).
     * </p>
     * 
     * @param preExistentCookies this must be either a
     *            <code>java.util.Map&lt;String, String&gt;</code> or a
     *            <code>Cookie[]</code>. It can be null.
     * @param cookieName the new cookie name to be added.
     * @param cookieValue the new cookie value to be added.
     */
    public static Object putAndMergeCookie(final Object preExistentCookies,
                                           final String cookieName,
                                           final String cookieValue)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, cookieName, cookieValue);
    }

    /**
     * <p>
     * Merges all the Cookies in newCookiesArray with the preExistentCookies, adding
     * the new ones and overwriting the existing ones (existing means same cookie
     * name).
     * </p>
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on Cookie[]).
     * </p>
     */
    public static Object putAndMergeCookie(final Object preExistentCookies, final Cookie[] newCookiesArray)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, newCookiesArray);
    }

    /**
     * <p>
     * Merges all the Cookies in newCookiesMap with the preExistentCookies, adding
     * the new ones and overwriting the existing ones (existing means same cookie
     * name).
     * </p>
     * <p>
     * It is <b>important</b> that you use the returned value of this method because
     * for some implementations of preExistentCookies it is not possible to add new
     * Cookies (for example, on Cookie[]).
     * </p>
     */
    public static Object putAndMergeCookie(final Object preExistentCookies,
                                           final Map<String, String> newCookiesMap)
    {
        return CookieStorageType.resolveCookieStorageType(preExistentCookies).putAndMergeCookie(
            preExistentCookies, newCookiesMap);
    }

    /**
     * Searches and return the cookie with the cookieName in the cookiesObject. It
     * returns <code>null</code> if the cookie is not present.
     */
    public static String getCookieValueFromCookies(final Object cookiesObject, final String cookieName)
    {
        return CookieStorageType.resolveCookieStorageType(cookiesObject).getCookieValueFromCookies(
            cookiesObject, cookieName);
    }

    /**
     * Returns an array view of the cookiesObject.
     */
    public static Cookie[] asArrayOfCookies(final Object cookiesObject)
    {
        return CookieStorageType.resolveCookieStorageType(cookiesObject).asArrayOfCookies(cookiesObject);
    }

    /**
     * Performs the right encoding based on whether {@link HttpMessage} is an
     * {@link HttpRequest} or an {@link HttpResponse}.
     */
    public static void addCookiesToHttpMessage(final Cookie[] cookies,
                                               final int cookieVersion,
                                               final HttpMessage httpMessage)
    {
        if (!ArrayUtils.isEmpty(cookies))
        {
            final boolean server = httpMessage instanceof HttpResponse;
            final CookieEncoder encoder = new CookieEncoder(server);
            for (final Cookie cookie : cookies)
            {
                cookie.setVersion(cookieVersion);
                encoder.addCookie(cookie);
            }
            httpMessage.setHeader(server ? "Set-Cookie" : "Cookie", encoder.encode());
        }
    }

}

/**
 * This enum type is here to distinguish and handle the two type of cookie storage
 * that we have. The method
 * {@link CookieStorageType#resolveCookieStorageType(Object)} allows you to select
 * the appropriate {@link CookieStorageType} for the cookiesObject that you have.
 */
enum CookieStorageType
{
    /**
     * <p>
     * This corresponds to the storage of cookies as a Cookie[].
     * </p>
     * <p>
     * All the parameters of type {@link Object} in the method of this object are
     * assumed to be of type Cookie[] and won't be checked. They will be cast to
     * Cookie[].
     * </p>
     */
    ARRAY_OF_COOKIES
    {
        @Override
        public Object putAndMergeCookie(final Object preExistentCookies,
                                        final String cookieName,
                                        final String cookieValue)
        {
            final Cookie[] preExistentCookiesArray = (Cookie[]) preExistentCookies;

            final int sessionIndex = getCookieIndexFromCookiesArray(cookieName, preExistentCookiesArray);

            // domain, path, secure (https) and expiry are handled in method
            // CookieHelper.addCookiesToClient()
            final Cookie newSessionCookie = new DefaultCookie(cookieName, cookieValue);
            final Cookie[] mergedCookiesArray;
            if (sessionIndex >= 0)
            {
                preExistentCookiesArray[sessionIndex] = newSessionCookie;
                mergedCookiesArray = preExistentCookiesArray;
            }
            else
            {
                final Cookie[] newSessionCookieArray = new Cookie[]{newSessionCookie};
                mergedCookiesArray = concatenateCookies(preExistentCookiesArray, newSessionCookieArray);
            }
            return mergedCookiesArray;
        }

        protected Cookie[] concatenateCookies(final Cookie[] cookies1, final Cookie[] cookies2)
        {
            if (cookies1 == null)
            {
                return cookies2;
            }
            else if (cookies2 == null)
            {
                return null;
            }
            else
            {
                final Cookie[] mergedCookies = new Cookie[cookies1.length + cookies2.length];
                System.arraycopy(cookies1, 0, mergedCookies, 0, cookies1.length);
                System.arraycopy(cookies2, 0, mergedCookies, cookies1.length, cookies2.length);
                return mergedCookies;
            }
        }

        protected int getCookieIndexFromCookiesArray(final String cookieName,
                                                     final Cookie[] preExistentCookies)
        {
            if (preExistentCookies != null && cookieName != null)
            {
                for (int i = 0; i < preExistentCookies.length; i++)
                {
                    if (cookieName.equals(preExistentCookies[i].getName()))
                    {
                        return i;
                    }
                }
            }
            return -1;
        }

        @Override
        public String getCookieValueFromCookies(final Object cookiesObject, final String cookieName)
        {
            final Cookie[] cookies = (Cookie[]) cookiesObject;

            final int sessionIndex = getCookieIndexFromCookiesArray(cookieName, cookies);
            if (sessionIndex >= 0)
            {
                return cookies[sessionIndex].getValue();
            }
            else
            {
                return null;
            }
        }

        @Override
        public void addCookiesToHttpMessage(final HttpMessage httpMessage,
                                            final Object cookiesObject,
                                            final int cookieVersion,
                                            final MuleEvent event)
        {
            CookieHelper.addCookiesToHttpMessage((Cookie[]) cookiesObject, cookieVersion, httpMessage);
        }

        @Override
        public Object putAndMergeCookie(final Object preExistentCookies, final Cookie[] newCookiesArray)
        {
            if (newCookiesArray == null)
            {
                return preExistentCookies;
            }
            final List<Cookie> cookiesThatAreReallyNew = new ArrayList<Cookie>(newCookiesArray.length);
            final Cookie[] preExistentCookiesArray = (Cookie[]) preExistentCookies;
            for (final Cookie newCookie : newCookiesArray)
            {
                final int newCookieInPreExistentArrayIndex = getCookieIndexFromCookiesArray(
                    newCookie.getName(), preExistentCookiesArray);
                if (newCookieInPreExistentArrayIndex >= 0)
                {
                    // overwrite the old one
                    preExistentCookiesArray[newCookieInPreExistentArrayIndex] = newCookie;
                }
                else
                {
                    // needs to add it at the end
                    cookiesThatAreReallyNew.add(newCookie);
                }
            }

            return concatenateCookies(preExistentCookiesArray,
                cookiesThatAreReallyNew.toArray(new Cookie[cookiesThatAreReallyNew.size()]));
        }

        @Override
        public Object putAndMergeCookie(final Object preExistentCookies,
                                        final Map<String, String> newCookiesMap)
        {
            if (newCookiesMap == null)
            {
                return putAndMergeCookie(preExistentCookies, (Cookie[]) null);
            }
            else
            {
                final Cookie[] cookiesArray = new Cookie[newCookiesMap.size()];
                int i = 0;
                for (final Entry<String, String> cookieEntry : newCookiesMap.entrySet())
                {
                    final Cookie cookie = new DefaultCookie(cookieEntry.getKey(), cookieEntry.getValue());
                    cookiesArray[i++] = cookie;
                }
                return putAndMergeCookie(preExistentCookies, cookiesArray);
            }
        }

        @Override
        public Cookie[] asArrayOfCookies(final Object cookiesObject)
        {
            if (cookiesObject == null)
            {
                return CookieHelper.EMPTY_COOKIE_ARRAY;
            }
            else
            {
                return (Cookie[]) cookiesObject;
            }
        }

    },

    /**
     * <p>
     * This corresponds to the storage of cookies as {@link Map<String, String>},
     * where the keys are the cookie names and the values are the cookie values.
     * </p>
     * <p>
     * All the parameters of type {@link Object} in the method of this object are
     * assumed to be of type {@link Map<String, String>} and won't be checked. They
     * will be cast to {@link Map} and used as if all the keys and values are of type
     * {@link String}.
     */
    MAP_STRING_STRING
    {
        @Override
        public Object putAndMergeCookie(final Object preExistentCookies,
                                        final String cookieName,
                                        final String cookieValue)
        {
            @SuppressWarnings("unchecked")
            final Map<String, String> cookieMap = (Map<String, String>) preExistentCookies;

            cookieMap.put(cookieName, cookieValue);
            return cookieMap;
        }

        @Override
        @SuppressWarnings("unchecked")
        public String getCookieValueFromCookies(final Object cookiesObject, final String cookieName)
        {
            return ((Map<String, String>) cookiesObject).get(cookieName);
        }

        @Override
        public void addCookiesToHttpMessage(final HttpMessage httpMessage,
                                            final Object cookiesObject,
                                            final int cookieVersion,
                                            final MuleEvent event)
        {
            @SuppressWarnings("unchecked")
            final Map<String, String> cookieMap = (Map<String, String>) cookiesObject;

            final List<Cookie> cookiesToAdd = new ArrayList<Cookie>();
            for (final Entry<String, String> cookie : cookieMap.entrySet())
            {
                final String cookieName = cookie.getKey();
                final String cookieValue = cookie.getValue();

                final String actualCookieValue;
                if (event != null)
                {
                    actualCookieValue = event.getMuleContext()
                        .getExpressionManager()
                        .parse(cookieValue, event);
                }
                else
                {
                    actualCookieValue = cookieValue;
                }

                cookiesToAdd.add(new DefaultCookie(cookieName, actualCookieValue));
            }

            CookieHelper.addCookiesToHttpMessage(cookiesToAdd.toArray(CookieHelper.EMPTY_COOKIE_ARRAY),
                cookieVersion, httpMessage);
        }

        @Override
        public Object putAndMergeCookie(final Object preExistentCookies, final Cookie[] newCookiesArray)
        {
            if (newCookiesArray == null)
            {
                return preExistentCookies;
            }

            Object mergedCookies = preExistentCookies;
            for (final Cookie cookie : newCookiesArray)
            {
                mergedCookies = putAndMergeCookie(mergedCookies, cookie.getName(), cookie.getValue());
            }
            return mergedCookies;
        }

        @Override
        public Object putAndMergeCookie(final Object preExistentCookies,
                                        final Map<String, String> newCookiesMap)
        {
            if (newCookiesMap == null)
            {
                return preExistentCookies;
            }

            Object mergedCookies = preExistentCookies;
            for (final Entry<String, String> cookieEntry : newCookiesMap.entrySet())
            {
                mergedCookies = putAndMergeCookie(mergedCookies, cookieEntry.getKey(), cookieEntry.getValue());
            }
            return mergedCookies;
        }

        @Override
        public Cookie[] asArrayOfCookies(final Object cookiesObject)
        {
            @SuppressWarnings("unchecked")
            final Map<String, String> cookieMap = (Map<String, String>) cookiesObject;
            final Cookie[] arrayOfCookies = new Cookie[cookieMap.size()];
            int i = 0;
            for (final Entry<String, String> cookieEntry : cookieMap.entrySet())
            {
                final Cookie cookie = new DefaultCookie(cookieEntry.getKey(), cookieEntry.getValue());
                arrayOfCookies[i++] = cookie;
            }
            return arrayOfCookies;
        }

    };

    /**
     * Resolves the cookiesObject to the appropriate {@link CookieStorageType}.
     * 
     * @param cookiesObject
     * @return
     */
    public static CookieStorageType resolveCookieStorageType(final Object cookiesObject)
    {
        if (cookiesObject == null || cookiesObject instanceof Cookie[])
        {
            return CookieStorageType.ARRAY_OF_COOKIES;
        }
        else if (cookiesObject instanceof Map)
        {
            return CookieStorageType.MAP_STRING_STRING;
        }
        else
        {
            throw new IllegalArgumentException("Invalid cookiesObject. Only " + Cookie.class + "[] and "
                                               + Map.class + " are supported: " + cookiesObject);
        }
    }

    /**
     * @see CookieHelper#putAndMergeCookie(Object, String, String)
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, String cookieName, String cookieValue);

    /**
     * @see CookieHelper#putAndMergeCookie(Object, Cookie[])
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, Cookie[] newCookiesArray);

    /**
     * @see CookieHelper#putAndMergeCookie(Object, Map)
     */
    public abstract Object putAndMergeCookie(Object preExistentCookies, Map<String, String> newCookiesMap);

    /**
     * @see CookieHelper#getCookieValueFromCookies(Object, String)
     */
    public abstract String getCookieValueFromCookies(Object cookiesObject, String cookieName);

    /**
     * @see CookieHelper#addCookiesToHttpMessage(HttpMessage, Object, String,
     *      MuleEvent, URI)
     */
    public abstract void addCookiesToHttpMessage(HttpMessage httpMessage,
                                                 Object cookiesObject,
                                                 int cookieVersion,
                                                 MuleEvent event);

    /**
     * @see CookieHelper#asArrayOfCookies(Object)
     */
    public abstract Cookie[] asArrayOfCookies(Object cookiesObject);
}

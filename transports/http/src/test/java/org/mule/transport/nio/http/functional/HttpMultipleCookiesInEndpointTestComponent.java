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

import org.jboss.netty.handler.codec.http.Cookie;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.transport.nio.http.CookieHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpMultipleCookiesInEndpointTestComponent implements Callable
{
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public Object onCall(final MuleEventContext muleEventContext) throws Exception
    {
        String response = "NO COOKIE FOUND!";

        final MuleMessage message = muleEventContext.getMessage();
        final Object cookiesProperty = message.getInboundProperty("cookies");

        logger.info("****************** Got cookies property: " + cookiesProperty.getClass().getName());

        final Cookie[] cookiesArray = CookieHelper.asArrayOfCookies(cookiesProperty);

        boolean cookie1Found = false;
        boolean cookie2Found = false;
        if (cookiesArray != null && cookiesArray.length > 0)
        {
            for (int i = 0; i < cookiesArray.length; i++)
            {
                final Cookie cookie = cookiesArray[i];

                logger.info("****************** (" + i + ") Got Cookie: " + cookie);

                if ("CookieNumber1".equals(cookie.getName())
                    && "ValueForCookieNumber1".equals(cookie.getValue()))
                {
                    cookie1Found = true;
                }
                else if ("CookieNumber2".equals(cookie.getName())
                         && "ValueForCookieNumber2".equals(cookie.getValue()))
                {
                    cookie2Found = true;
                }
            }
        }
        if (cookie1Found && cookie2Found)
        {
            response = "Both Cookies Found!";
        }
        else if (cookie1Found)
        {
            response = "Only cookie1 was found";
        }
        else if (cookie2Found)
        {
            response = "Only cookie2 was found";
        }

        return response;
    }
}

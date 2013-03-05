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

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;

/**
 * A wrapper for helping building {@link Cookie}s.
 */
public class CookieWrapper implements Serializable
{
    private static final long serialVersionUID = -6389743235632180272L;

    private String name, value, domain, path;
    private Object expiryDate;
    private String maxAge;
    private String secure;
    private String version;

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public void parse(final MuleEvent event)
    {
        setName(parse(getName(), event));
        setValue(parse(getValue(), event));
        this.domain = parse(domain, event);
        this.path = parse(path, event);
        if (expiryDate != null)
        {
            this.expiryDate = evaluateDate(expiryDate, event);
        }
        this.maxAge = parse(maxAge, event);
        this.secure = parse(secure, event);
        this.version = parse(version, event);
    }

    private String parse(final String value, final MuleEvent event)
    {
        final ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();

        if ((StringUtils.isNotBlank(value)) && (expressionManager.isExpression(value)))
        {
            return expressionManager.parse(value, event);
        }

        return value;
    }

    private Object evaluateDate(final Object date, final MuleEvent event)
    {
        final ExpressionManager expressionManager = event.getMuleContext().getExpressionManager();

        if ((date instanceof String) && (expressionManager.isExpression((String) date)))
        {
            return expressionManager.evaluate(date.toString(), event);
        }

        return date;
    }

    public Cookie createCookie(final int defaultCookieVersion) throws ParseException
    {
        final Cookie cookie = new DefaultCookie(getName(), getValue());
        cookie.setDomain(domain);
        cookie.setPath(path);

        if (expiryDate != null)
        {
            cookie.setMaxAge(safeLongToInt((formatExpiryDate(expiryDate).getTime() - System.currentTimeMillis()) / 1000L));
        }

        if (maxAge != null && expiryDate == null)
        {
            cookie.setMaxAge(Integer.valueOf(maxAge));
        }

        if (secure != null)
        {
            cookie.setSecure(Boolean.valueOf(secure));
        }
        if (version != null)
        {
            cookie.setVersion(Integer.valueOf(version));
        }
        else
        {
            cookie.setVersion(defaultCookieVersion);
        }
        return cookie;
    }

    private Date formatExpiryDate(final Object expiryDate) throws ParseException
    {
        if (expiryDate instanceof String)
        {
            final SimpleDateFormat format = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("GMT"));
            return format.parse((String) expiryDate);
        }
        return (Date) expiryDate;
    }

    // from
    // http://stackoverflow.com/questions/1590831/safely-casting-long-to-int-in-java
    private static int safeLongToInt(final long l)
    {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE)
        {
            throw new IllegalArgumentException(l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }

    public String getName()
    {
        return name;
    }

    public void setName(final String name)
    {
        this.name = name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(final String value)
    {
        this.value = value;
    }

    public void setDomain(final String domain)
    {
        this.domain = domain;
    }

    public void setPath(final String path)
    {
        this.path = path;
    }

    public void setExpiryDate(final Object expiryDate)
    {
        this.expiryDate = expiryDate;
    }

    public void setMaxAge(final String maxAge)
    {
        this.maxAge = maxAge;
    }

    public void setSecure(final String secure)
    {
        this.secure = secure;
    }

    public void setVersion(final String version)
    {
        this.version = version;
    }
}

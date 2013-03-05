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

import java.util.Arrays;

import org.mule.api.MuleEvent;

/**
 * Includes basic configuration for the HTTP Cache-Control Header
 */
public class CacheControlHeader
{
    private static final String[] DIRECTIVE = {"public", "private"};

    private String directive;
    private String noCache;
    private String noStore;
    private String mustRevalidate;
    private String maxAge;

    public CacheControlHeader()
    {
        noCache = "false";
        noStore = "false";
        mustRevalidate = "false";
    }

    /**
     * Evaluates all the properties in case there are expressions
     * 
     * @param message MuleMessage
     * @param expressionManager
     */
    public void parse(final MuleEvent event)
    {
        directive = parse(directive, event);
        checkDirective(directive);
        noCache = parse(noCache, event);
        noStore = parse(noStore, event);
        mustRevalidate = parse(mustRevalidate, event);
        maxAge = parse(maxAge, event);
    }

    private void checkDirective(final String directive)
    {
        if (directive != null && !Arrays.asList(DIRECTIVE).contains(directive))
        {
            throw new IllegalArgumentException("Invalid Cache-Control directive: " + directive);
        }
    }

    @Override
    public String toString()
    {
        final StringBuffer cacheControl = new StringBuffer("");
        if (directive != null)
        {
            cacheControl.append(directive).append(",");
        }
        if (Boolean.valueOf(noCache))
        {
            cacheControl.append("no-cache").append(",");
        }
        if (Boolean.valueOf(noStore))
        {
            cacheControl.append("no-store").append(",");
        }
        if (Boolean.valueOf(mustRevalidate))
        {
            cacheControl.append("must-revalidate").append(",");
        }
        if (maxAge != null)
        {
            cacheControl.append("max-age=").append(maxAge).append(",");
        }

        final String value = cacheControl.toString();
        if (value.endsWith(","))
        {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String parse(final String value, final MuleEvent event)
    {
        if (value != null)
        {
            return event.getMuleContext().getExpressionManager().parse(value, event);
        }
        return value;
    }

    public void setDirective(final String directive)
    {
        this.directive = directive;
    }

    public void setNoCache(final String noCache)
    {
        this.noCache = noCache;
    }

    public void setNoStore(final String noStore)
    {
        this.noStore = noStore;
    }

    public void setMustRevalidate(final String mustRevalidate)
    {
        this.mustRevalidate = mustRevalidate;
    }

    public void setMaxAge(final String maxAge)
    {
        this.maxAge = maxAge;
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.config;

import org.mule.api.config.MuleProperties;
import org.mule.util.SpiTransposer;

public class HttpSpiTransposer implements SpiTransposer
{
    private static final HttpSpiTransposer instance = new HttpSpiTransposer();
    
    public static final HttpSpiTransposer getInstance()
    {
        return instance;
    }
    
    private HttpSpiTransposer()
    {
    }

    /* (non-Javadoc)
     * @see org.mule.util.SpiTransposer#isNameTransposible(java.lang.String)
     */
    @Override
    public boolean isNameTransposible(String name)
    {
        return name != null && name.toLowerCase().startsWith("http") && !name.toLowerCase().startsWith("https");
    }

    /* (non-Javadoc)
     * @see org.mule.util.SpiTransposer#transposeName(java.lang.String)
     */
    @Override
    public String transposeName(String name)
    {
        if (isNioEnabled())
        {
            return name.replaceFirst("http", "niohttp");
        }
        return name;
    }

    protected boolean isNioEnabled()
    {
        return Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
    }
}



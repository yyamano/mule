/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.config;

import org.mule.api.config.MuleProperties;
import org.mule.util.SpiTransposer;

public class TcpSpiTransposer implements SpiTransposer
{
    private static final TcpSpiTransposer instance = new TcpSpiTransposer();
    
    public static final TcpSpiTransposer getInstance()
    {
        return instance;
    }
    
    private TcpSpiTransposer()
    {
    }
    
    /* (non-Javadoc)
     * @see org.mule.util.SpiTransposer#isNameTransposible(java.lang.String)
     */
    @Override
    public boolean isNameTransposible(String name)
    {
        return name != null && name.toLowerCase().startsWith("tcp");
    }

    /* (non-Javadoc)
     * @see org.mule.util.SpiTransposer#transposeName(java.lang.String)
     */
    @Override
    public String transposeName(String name)
    {
        if (isNioEnabled())
        {
            return name.replaceFirst("tcp", "niotcp");
        }
        return name;
    }

    protected boolean isNioEnabled()
    {
        return Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
    }
}    



/*
 * $Id: TcpEndpointURIBuilder.java 25306 2013-02-26 01:26:25Z cmordue $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.endpoint.SocketEndpointURIBuilder;

import java.net.URI;
import java.net.URISyntaxException;

public class TcpEndpointURIBuilder extends SocketEndpointURIBuilder
{
    @Override
    public EndpointURI build(URI uri, MuleContext muleContext) throws MalformedEndpointException
    {
        if (isNioEnabled(muleContext))
        {
            String scheme = getNioSchema(uri);
            try
            {
                String newUriString = uri.toASCIIString().replaceFirst(uri.getScheme(), scheme);
                uri = new URI(newUriString);
            }
            catch (URISyntaxException e)
            {
                throw new MalformedEndpointException(e);
            }
        }
        
        return super.build(uri, muleContext);
    }
    
    protected String getNioSchema(URI uri)
    {
        String schema = uri.getScheme();
        if ("tcp".equalsIgnoreCase(schema))
        {
            schema = "niotcp";
        }
        return schema;
    }
    
    protected boolean isNioEnabled(MuleContext muleContext)
    {
        return Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
    }
}



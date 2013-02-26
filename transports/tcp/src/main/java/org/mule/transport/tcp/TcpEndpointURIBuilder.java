/*
 * $Id$
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
            if (!"tcp".equalsIgnoreCase(uri.getScheme()))
            {
                System.out.println("TcpEndpointURIBuilder is trying build an endpoint for a non-tcp scheme. Is this a bug?");
            }
            String scheme = "niotcp";
            try
            {
                uri = new URI(scheme, uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment());
            }
            catch (URISyntaxException e)
            {
                throw new MalformedEndpointException(e);
            }
        }
        
        return super.build(uri, muleContext);
    }
    
    protected boolean isNioEnabled(MuleContext muleContext)
    {
        String isNioEnabledStr = muleContext.getRegistry().get(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
        return Boolean.parseBoolean(isNioEnabledStr) || Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
    }
}



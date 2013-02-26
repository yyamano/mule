/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connectable;
import org.mule.util.ClassUtils;

/**
 * This is used to adapt an endpoint so that it can be used as a key for
 * {@link TcpClient}s.
 */
public class TcpClientKey
{
    private final Connectable connectable;
    private final ImmutableEndpoint endpoint;
    private final int hashCode;

    public TcpClientKey(final Connectable connectable, final ImmutableEndpoint endpoint)
    {
        Validate.notNull(connectable, "connectable can't be null");
        Validate.notNull(endpoint, "endpoint can't be null");

        this.connectable = connectable;
        this.endpoint = endpoint;

        hashCode = computeHash(endpoint);
    }

    protected int computeHash(final ImmutableEndpoint endpoint)
    {
        final EndpointURI endpointURI = endpoint.getEndpointURI();
        return ClassUtils.hash(new Object[]{endpointURI.getScheme(),
            endpointURI.getHost(), endpointURI.getPort()});
    }

    @Override
    public boolean equals(final Object obj)
    {
        return obj instanceof TcpClientKey && computeHash(((TcpClientKey) obj).getEndpoint()) == hashCode;
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    public Connectable getConnectable()
    {
        return connectable;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }
}

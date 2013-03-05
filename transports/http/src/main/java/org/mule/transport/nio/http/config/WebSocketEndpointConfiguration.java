/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.config;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketVersion;
import org.mule.util.BeanUtils;

/**
 * WebSocket related configuration that needs to be defined on HTTP endpoints.
 */
public class WebSocketEndpointConfiguration implements Serializable
{
    protected String group = null;
    protected WebSocketVersion version = WebSocketVersion.V13;
    protected String subprotocols = null;
    protected boolean allowExtensions = false;
    protected long maxFramePayloadLength = Long.MAX_VALUE;
    protected Map<String, String> handshakeHeaders = null;

    public WebSocketEndpointConfiguration()
    {
        // NOOP
    }

    public WebSocketEndpointConfiguration(final Map<?, ?> props)
    {
        BeanUtils.populateWithoutFail(this, props, false);
        final Object versionProp = props.get("version");
        if (versionProp != null)
        {
            setVersion(WebSocketVersion.valueOf(versionProp.toString()));
        }
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(final String group)
    {
        Validate.notEmpty(group, "group can't be empty");
        this.group = group;
    }

    public WebSocketVersion getVersion()
    {
        return version;
    }

    public void setVersion(final WebSocketVersion version)
    {
        this.version = version;
    }

    public String getSubprotocols()
    {
        return subprotocols;
    }

    public void setSubprotocols(final String subprotocols)
    {
        this.subprotocols = subprotocols;
    }

    public boolean isAllowExtensions()
    {
        return allowExtensions;
    }

    public void setAllowExtensions(final boolean allowExtensions)
    {
        this.allowExtensions = allowExtensions;
    }

    public long getMaxFramePayloadLength()
    {
        return maxFramePayloadLength;
    }

    public void setMaxFramePayloadLength(final long maxFramePayloadLength)
    {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }

    public Map<String, String> getHandshakeHeaders()
    {
        return handshakeHeaders;
    }

    public void setHandshakeHeaders(final Map<String, String> handshakeHeaders)
    {
        this.handshakeHeaders = handshakeHeaders;
    }
}

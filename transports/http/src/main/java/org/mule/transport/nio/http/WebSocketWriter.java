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

import java.util.Collection;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.util.NumberUtils;
import org.mule.util.StringUtils;

/**
 * A {@link MessageProcessor} that can write an event to a single or a group of
 * websockets handled by an {@link HttpConnector}.
 */
public class WebSocketWriter implements MessageProcessor
{
    private HttpConnector connector;
    private String group;
    private String channelIdExpression;
    private boolean failIfNoWrite;

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        if (!dispatch(event) && isFailIfNoWrite())
        {
            throw new MessagingException(HttpMessages.websocketDispatchFailed(), event);
        }
        return event;
    }

    protected boolean dispatch(final MuleEvent event) throws MessagingException
    {
        final HttpConnector httpConnector = getDispatchingHttpConnector(event);
        try
        {
            int totalDispatched;
            totalDispatched = httpConnector.writeToWebSocket(event, group, getTargetChannelId(event));
            return totalDispatched > 0;
        }
        catch (final Exception e)
        {
            throw new MessagingException(CoreMessages.eventProcessingFailedFor(toString()), event, e);
        }
    }

    protected HttpConnector getDispatchingHttpConnector(final MuleEvent event) throws MessagingException
    {
        if (connector != null)
        {
            return connector;
        }

        final Collection<HttpConnector> httpConnectors = event.getMuleContext()
            .getRegistry()
            .lookupObjects(HttpConnector.class);

        if (httpConnectors.isEmpty())
        {
            return null;
        }
        else if (httpConnectors.size() > 1)
        {
            throw new MessagingException(CoreMessages.moreThanOneConnectorWithProtocol(HttpConnector.PROTOCOL,
                httpConnectors.toString()), event);
        }
        else
        {
            return httpConnectors.iterator().next();
        }
    }

    protected Integer getTargetChannelId(final MuleEvent event)
    {
        if (StringUtils.isBlank(channelIdExpression))
        {
            return null;
        }
        if (StringUtils.isNumeric(channelIdExpression))
        {
            return Integer.valueOf(channelIdExpression);
        }

        return NumberUtils.toInt(event.getMuleContext()
            .getExpressionManager()
            .evaluate(channelIdExpression, event));
    }

    public HttpConnector getConnector()
    {
        return connector;
    }

    public void setConnector(final HttpConnector connector)
    {
        this.connector = connector;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(final String group)
    {
        this.group = group;
    }

    public boolean isFailIfNoWrite()
    {
        return failIfNoWrite;
    }

    public void setFailIfNoWrite(final boolean failIfNoWrite)
    {
        this.failIfNoWrite = failIfNoWrite;
    }

    public void setChannelId(final String channelIdExpression)
    {
        this.channelIdExpression = channelIdExpression;
    }

    public String getChannelId()
    {
        return channelIdExpression;
    }
}

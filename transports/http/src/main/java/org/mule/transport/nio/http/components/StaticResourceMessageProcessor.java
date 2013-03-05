/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.components;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.transport.NullPayload;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.i18n.HttpMessages;
import org.mule.util.IOUtils;
import org.mule.util.StringUtils;

/**
 * A MessageProcessor that can be used by HTTP endpoints to serve static files from a
 * directory on the filesystem. This processor allows the user to specify a
 * resourceBase which refers to the local directory from where files will be served
 * from. Additionally, a default file can be specificed for URLs where no file is set
 */
public class StaticResourceMessageProcessor implements MessageProcessor, Initialisable
{
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private String resourceBase;
    private String defaultFile = "index.html";
    private MimetypesFileTypeMap mimeTypes;

    public void initialise() throws InitialisationException
    {
        mimeTypes = new MimetypesFileTypeMap();
        mimeTypes.addMimeTypes("text/javascript js");
        mimeTypes.addMimeTypes("text/css css");
    }

    public MuleEvent process(final MuleEvent event) throws MuleException
    {
        if (StringUtils.isEmpty(resourceBase))
        {
            throw new ConfigurationException(HttpMessages.noResourceBaseDefined());
        }

        String path = event.getMessage().getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY);
        final String contextPath = event.getMessage().getInboundProperty(
            HttpConnector.HTTP_CONTEXT_PATH_PROPERTY);

        // Remove the contextPath from the endpoint from the request as this isn't
        // part of the path.
        path = path.substring(contextPath.length());

        File file = new File(resourceBase + path);
        MuleEvent resultEvent = event;

        if (file.isDirectory() && path.endsWith("/"))
        {
            file = new File(resourceBase + path + defaultFile);
        }
        else if (file.isDirectory())
        {
            // Return a 302 with the new location
            resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(NullPayload.getInstance(),
                event.getMuleContext()), event);
            resultEvent.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                String.valueOf(HttpConstants.SC_MOVED_TEMPORARILY));
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, 0);
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_LOCATION,
                event.getMessage().getInboundProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY) + "/");
            return resultEvent;
        }

        try
        {
            final InputStream in = new FileInputStream(file);
            final byte[] buffer = IOUtils.toByteArray(in);
            IOUtils.closeQuietly(in);

            String mimetype = mimeTypes.getContentType(file);
            if (mimetype == null)
            {
                mimetype = DEFAULT_MIME_TYPE;
            }

            resultEvent = new DefaultMuleEvent(new DefaultMuleMessage(buffer, event.getMuleContext()), event);
            resultEvent.getMessage().setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY,
                String.valueOf(HttpConstants.SC_OK));
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, mimetype);
            resultEvent.getMessage().setOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH, buffer.length);
            return resultEvent;
        }
        catch (final FileNotFoundException fnfe)
        {
            throw new ResourceNotFoundException(HttpMessages.fileNotFound(resourceBase + path), event);
        }
    }

    public String getResourceBase()
    {
        return resourceBase;
    }

    public void setResourceBase(final String resourceBase)
    {
        this.resourceBase = resourceBase;
    }

    public String getDefaultFile()
    {
        return defaultFile;
    }

    public void setDefaultFile(final String defaultFile)
    {
        this.defaultFile = defaultFile;
    }
}

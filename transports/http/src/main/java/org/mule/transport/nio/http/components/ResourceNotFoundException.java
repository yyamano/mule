/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.components;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

/**
 * Thrown when a static file is requested but not found.
 */
public class ResourceNotFoundException extends MessagingException
{
    private static final long serialVersionUID = -6693780652453067695L;

    public ResourceNotFoundException(final Message message, final MuleEvent event)
    {
        super(message, event);
    }
}

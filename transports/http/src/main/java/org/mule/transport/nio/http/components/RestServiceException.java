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

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.config.i18n.Message;

/**
 * Thrown when an error occurs while interacting with a REST resource.
 */
public class RestServiceException extends MessagingException
{
    private static final long serialVersionUID = -1026055907767407435L;

    public RestServiceException(final Message message, final MuleEvent event)
    {
        super(message, event);
    }

    public RestServiceException(final Message message, final MuleEvent event, final Throwable cause)
    {
        super(message, event, cause);
    }
}

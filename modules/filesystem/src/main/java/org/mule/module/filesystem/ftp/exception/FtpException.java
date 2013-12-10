/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.ftp.exception;

import org.mule.api.MuleException;
import org.mule.config.i18n.Message;

public class FtpException extends MuleException
{

    private static final long serialVersionUID = 4069885904379528858L;

    public FtpException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}

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

import org.mule.api.MuleException;
import org.mule.transport.nio.http.i18n.HttpMessages;

/**
 * A wrapper exception for any HTTP client return codes over the 400 range.
 */
public class HttpResponseException extends MuleException
{
    private static final long serialVersionUID = -8369957827845641946L;

    private final String responseText;
    private final int responseCode;

    public HttpResponseException(final String responseText, final int responseCode)
    {
        super(HttpMessages.requestFailedWithStatus(responseText + ", code: " + responseCode));
        this.responseCode = responseCode;
        this.responseText = responseText;
    }

    public String getResponseText()
    {
        return responseText;
    }

    public int getResponseCode()
    {
        return responseCode;
    }
}

/*
 * $Id: HttpOutboundHeadersPropagationComponent.java 24173 2012-03-26 15:40:11Z saveli.vassiliev $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.functional;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOutboundHeadersPropagationComponent implements Callable
{

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public Object onCall(MuleEventContext muleEventContext) throws Exception
    {
        MuleMessage m = muleEventContext.getMessage();
        Map<String, Object> headers = new TreeMap<String, Object>();
        for(String s : m.getInboundPropertyNames())
        {
            headers.put(s, m.getInboundProperty(s));
        }
        return headers;
    }
}

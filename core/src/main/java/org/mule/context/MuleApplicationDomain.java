/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context;

public class MuleApplicationDomain
{

    private String domain;
    private Object context;

    public MuleApplicationDomain(String domain, Object context)
    {
        this.domain = domain;
        this.context = context;
    }

    public String getDomain()
    {
        return domain;
    }

    public Object getContext()
    {
        return context;
    }
}

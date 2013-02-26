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

import org.mule.api.config.MuleProperties;
import org.mule.tck.junit4.rule.SystemProperty;

public class NioProperty extends SystemProperty
{
    public NioProperty(boolean isNioEnabled)
    {
        super(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY, Boolean.toString(isNioEnabled));
    }
}



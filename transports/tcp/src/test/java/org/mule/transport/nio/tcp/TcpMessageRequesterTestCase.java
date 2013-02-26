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

import org.junit.Rule;

@NioTest
public class TcpMessageRequesterTestCase extends org.mule.transport.tcp.TcpMessageRequesterTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Override
    protected int getWaitForeverTimeoutValue()
    {
        return -1;
    }
}



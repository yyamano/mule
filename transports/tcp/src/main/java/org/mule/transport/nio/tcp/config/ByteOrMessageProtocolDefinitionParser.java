/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.tcp.config;

import org.mule.api.MuleMessage;
import org.mule.config.spring.parsers.delegate.BooleanAttributeSelectionDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.transport.nio.tcp.TcpProtocol;
import org.springframework.beans.factory.xml.BeanDefinitionParser;

/**
 * A {@link BeanDefinitionParser} that supports elements defining {@link TcpProtocol}
 * s with two options: dealing with the {@link MuleMessage}'s payload only or with
 * the {@link MuleMessage} as a whole.
 */
public class ByteOrMessageProtocolDefinitionParser extends BooleanAttributeSelectionDefinitionParser
{
    public static final String PROTOCOL = "tcpProtocol";

    public ByteOrMessageProtocolDefinitionParser(final Class<?> bytes, final Class<?> message)
    {
        super("payloadOnly", true, new ChildDefinitionParser(PROTOCOL, bytes), new ChildDefinitionParser(
            PROTOCOL, message));
    }
}

/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.tcp.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.MuleDefinitionParser;
import org.mule.config.spring.parsers.delegate.AbstractParallelDelegatingDefinitionParser;

import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class NioSelectorDelegatingDefinitionParser extends AbstractParallelDelegatingDefinitionParser
{

    private MuleDefinitionParser whenTrue;
    private MuleDefinitionParser whenFalse;
    
    public NioSelectorDelegatingDefinitionParser(MuleDefinitionParser whenTrue, MuleDefinitionParser whenFalse)
    {
        super(new MuleDefinitionParser[]{whenTrue, whenFalse});
        this.whenTrue = whenTrue;
        this.whenFalse = whenFalse;
    }

    @Override
    protected MuleDefinitionParser getDelegate(Element element, ParserContext parserContext)
    {
        if (Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY))
        {
            return whenTrue;
        }
        else
        {
            return whenFalse;
        }
    }

}



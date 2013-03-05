/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.config;

import org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser;

import org.w3c.dom.Element;

public class RuntimeExceptionDefinitionParser extends AbstractMuleBeanDefinitionParser
{
    private RuntimeException runtimeException;
    
    public RuntimeExceptionDefinitionParser(RuntimeException runtimeException)
    {
        this.runtimeException = runtimeException;
    }

    /* (non-Javadoc)
     * @see org.mule.config.spring.parsers.AbstractMuleBeanDefinitionParser#getBeanClass(org.w3c.dom.Element)
     */
    @Override
    protected Class<?> getBeanClass(Element element)
    {
        throw runtimeException;
    }
}



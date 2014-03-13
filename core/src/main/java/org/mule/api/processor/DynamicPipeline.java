/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleException;

public interface DynamicPipeline
{
    void addPreMessageProcessor(final MessageProcessor preMessageProcessor) throws MuleException;

    void removePreMessageProcessor(final MessageProcessor preMessageProcessor) throws MuleException;

    void addPostMessageProcessor(final MessageProcessor postMessageProcessor) throws MuleException;

    void removePostMessageProcessor(final MessageProcessor postMessageProcessor) throws MuleException;

    void build() throws MuleException;
}

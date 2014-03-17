/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleException;

/**
 * Adds to a pipeline the ability to dynamically inject a sequence
 * of message processors after initialization.
 *
 * The injected message processors are executed before the ones defined in
 * the flow in the specified order.
 *
 * To execute a message processor after the ones in the flow it needs to
 * be wrapped with a {@link org.mule.processor.ResponseMessageProcessorAdapter}.
 * The wrapped message processors are executed in reverse order.
 *
 */
public interface DynamicPipeline
{

    void updateChain(MessageProcessor... messageProcessors) throws MuleException;
}

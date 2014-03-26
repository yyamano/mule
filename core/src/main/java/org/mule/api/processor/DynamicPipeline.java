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
 * The injected message processors are executed before (pre) of after (post)
 * the ones defined in the flow in the specified order.
 *
 */
public interface DynamicPipeline
{

    /**
     * Adds a message processor to be run before the flow static pipeline.
     *
     * @param messageProcessor
     * @throws MuleException
     */
    void addPreMessageProcessor(MessageProcessor messageProcessor) throws MuleException;

    /**
     * Adds a message processor to be run after the flow static pipeline.
     *
     * @param messageProcessor
     * @throws MuleException
     */
    void addPostMessageProcessor(MessageProcessor messageProcessor) throws MuleException;

    /**
     * Updates the pipeline with the added message processors.
     * Updating the pipeline more than once requires adding all necessary
     * message processors again.
     *
     * @throws MuleException
     */
    void updatePipeline() throws MuleException;

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.processor;

import org.mule.api.MuleException;

import java.util.List;

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
     * Updates the pipeline injecting the lists of preMessageProcessors and postMessageProcessors.
     *
     * @param preMessageProcessors message processors to be executed before the ones specified in the flow
     * @param postMessageProcessors message processors to be executed after the ones specified in the flow
     * @throws MuleException
     */
    void updatePipeline(List<MessageProcessor> preMessageProcessors, List<MessageProcessor> postMessageProcessors) throws MuleException;

    /**
     * Removes all injected message processors.
     *
     * @throws MuleException
     */
    void resetPipeline() throws MuleException;

    /**
     * Helper builder for injecting message processors to be executed
     * before the ones specified in the flow.
     * After adding all required message processors #updatePipeline()
     * must be called.
     *
     * @param messageProcessors message processors to be executed before the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipeline injectBefore(MessageProcessor... messageProcessors);

    /**
     * Helper builder for injecting message processors to be executed
     * after the ones specified in the flow.
     * After adding all required message processors #updatePipeline()
     * must be called.
     *
     * @param messageProcessors message processors to be executed after the ones specified in the flow
     * @return the pipeline injector builder instance
     */
    DynamicPipeline injectAfter(MessageProcessor... messageProcessors);

    /**
     * Injects the message processors added with #injectBefore() and #injectAfter()
     * If none were added the effect is the same as calling #resetPipeline()
     *
     * @throws MuleException
     */
    void updatePipeline() throws MuleException;

}

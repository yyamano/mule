/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.construct;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.api.processor.DynamicPipeline;
import org.mule.api.processor.InterceptingMessageProcessor;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.AbstractMessageProcessorChain;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.processor.chain.SimpleMessageProcessorChain;

import java.util.ArrayList;
import java.util.List;

class DynamicPipelineMessageProcessor extends AbstractInterceptingMessageProcessor implements DynamicPipeline
{

    private List<MessageProcessor> preMessageProcessors = new ArrayList<MessageProcessor>();
    private List<MessageProcessor> postMessageProcessors = new ArrayList<MessageProcessor>();

    private AbstractMessageProcessorChain preChain;
    private AbstractMessageProcessorChain postChain;

    private MessageProcessor staticChain;
    private Flow flow;

    public DynamicPipelineMessageProcessor(Flow flow)
    {
        this.flow = flow;
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        return processNext(event);
    }

    @Override
    public void setListener(MessageProcessor next)
    {
        if (staticChain == null)
        {
            if (next instanceof InterceptingMessageProcessor)
            {
                //wrap with chain to avoid intercepting the postChain
                staticChain = new SimpleMessageProcessorChain(next);
            }
            else
            {
                staticChain = next;
            }
        }
        super.setListener(next);
    }

    @Override
    public void addPreMessageProcessor(MessageProcessor messageProcessor) throws MuleException
    {
        preMessageProcessors.add(messageProcessor);
    }

    @Override
    public void addPostMessageProcessor(MessageProcessor messageProcessor) throws MuleException
    {
        postMessageProcessors.add(messageProcessor);
    }

    @Override
    public void updatePipeline() throws MuleException
    {
        //build new dynamic chains
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder(flow);
        builder.chain(preMessageProcessors);

        builder.chain(staticChain);
        builder.chain(postMessageProcessors);
        MessageProcessorChain newChain = builder.build();

        Lifecycle preChainOld = preChain;
        Lifecycle postChainOld = postChain;
        initDynamicChains();

        //hook chain as last step to avoid synchronization
        super.setListener(newChain);

        //dispose old dynamic chains
        disposeDynamicChains(preChainOld, postChainOld);
    }

    private void initDynamicChains() throws InitialisationException
    {
        preChain = new SimpleMessageProcessorChain(preMessageProcessors);
        postChain = new SimpleMessageProcessorChain(postMessageProcessors);
        for (Lifecycle chain : new Lifecycle[] {preChain, postChain})
        {
            if (chain != null)
            {
                flow.injectFlowConstructMuleContext(chain);
                flow.initialiseIfInitialisable(chain);
            }
        }
    }

    private void disposeDynamicChains(Lifecycle... chains) throws MuleException
    {
        for (Lifecycle chain : chains)
        {
            if (chain != null)
            {
                chain.stop();
                chain.dispose();
            }
        }
        preMessageProcessors.clear();
        postMessageProcessors.clear();
    }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.lifecycle.Callable;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class DynamicFlowTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/construct/dynamic-flow.xml";
    }

    @Test
    public void addPreMessageProccesor() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(static)", result.getPayloadAsString());

        Flow flow = getFlow("dynamicFlow");
        flow.injectBefore(new StringAppendTransformer("(pre)")).updatePipeline();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());

        flow.injectBefore(new StringAppendTransformer("(pre1)"), new StringAppendTransformer("(pre2)")).updatePipeline();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre1)(pre2)(static)", result.getPayloadAsString());
    }

    @Test
    public void addPrePostMessageProccesor() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Flow flow = getFlow("dynamicFlow");
        flow.injectBefore(new StringAppendTransformer("(pre)"))
                .injectAfter(new StringAppendTransformer("(post)"))
                .updatePipeline();
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post)", result.getPayloadAsString());

        flow.injectBefore(new StringAppendTransformer("(pre)"))
                .injectAfter(new StringAppendTransformer("(post1)"), new StringAppendTransformer("(post2)"))
                .updatePipeline();
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post1)(post2)", result.getPayloadAsString());
    }

    @Test
    public void dynamicComponent() throws Exception
    {
        MuleClient client = muleContext.getClient();

        //invocation #1
        MuleMessage result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->(static)", result.getPayloadAsString());

        //invocation #2
        result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->chain update #1(static)", result.getPayloadAsString());

        //invocation #3
        result = client.send("vm://dynamicComponent", "source->", null);
        assertEquals("source->chain update #2(static)", result.getPayloadAsString());
    }

    @Test
    public void exceptionOnInjectedMessageProcessor() throws Exception
    {
        MuleClient client = muleContext.getClient();
        List<MessageProcessor> preList = new ArrayList<MessageProcessor>();
        List<MessageProcessor> postList = new ArrayList<MessageProcessor>();

        Flow flow = getFlow("exceptionFlow");
        preList.add(new StringAppendTransformer("(pre)"));
        preList.add(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new RuntimeException("force exception!");
            }
        });
        postList.add(new StringAppendTransformer("(post)"));
        flow.updatePipeline(preList, postList);
        MuleMessage result = client.send("vm://exception", "source->", null);
        assertEquals("source->(pre)(handled)", result.getPayloadAsString());
    }

    public static class Component implements Callable
    {

        private int count;

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Flow flow = (Flow) eventContext.getMuleContext().getRegistry().lookupFlowConstruct("dynamicComponentFlow");
            flow.injectBefore(new StringAppendTransformer("chain update #" + ++count)).updatePipeline();
            return eventContext.getMessage();
        }
    }

    private static Flow getFlow(String flowName) throws MuleException
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
    }
}

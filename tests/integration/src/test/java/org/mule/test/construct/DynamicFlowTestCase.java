package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleContext;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.Callable;
import org.mule.construct.Flow;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

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

        getFlow().updateChain(new StringAppendTransformer("(pre)"));
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)", result.getPayloadAsString());

        getFlow().updateChain(new StringAppendTransformer("(pre1)"), new StringAppendTransformer("(pre2)"));
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre1)(pre2)(static)", result.getPayloadAsString());
    }

    @Test
    public void addPrePostMessageProccesor() throws Exception
    {
        MuleClient client = muleContext.getClient();

        getFlow().updateChain(new StringAppendTransformer("(pre)"),
                              new ResponseMessageProcessorAdapter(new StringAppendTransformer("(post)")));
        MuleMessage result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post)", result.getPayloadAsString());

        getFlow().updateChain(new StringAppendTransformer("(pre)"),
                              new ResponseMessageProcessorAdapter(new StringAppendTransformer("(post1)")),
                              new ResponseMessageProcessorAdapter(new StringAppendTransformer("(post2)")));
        result = client.send("vm://dynamic", "source->", null);
        assertEquals("source->(pre)(static)(post2)(post1)", result.getPayloadAsString());
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

    public static class Component implements Callable
    {

        private int count;

        @Override
        public Object onCall(MuleEventContext eventContext) throws Exception
        {
            Flow flow = (Flow) eventContext.getMuleContext().getRegistry().lookupFlowConstruct("dynamicComponentFlow");
            flow.updateChain(new StringAppendTransformer("chain update #" + ++count));
            return eventContext.getMessage();
        }
    }

    private static Flow getFlow() throws MuleException
    {
        return (Flow) muleContext.getRegistry().lookupFlowConstruct("dynamicFlow");
    }
}

package org.mule.test.integration.domain;

import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.DomainFunctionalTestCase;

import java.util.Map;

import org.hamcrest.core.Is;
import org.junit.Test;

public class VmInterAppCommunicationTestCase extends DomainFunctionalTestCase
{

    @Override
    protected String getDomainConfig()
    {
        return "domain/vm-shared-connector.xml";
    }

    @Override
    public String[] getConfigResources()
    {
        return new String[] {
                "domain/vm-client-app.xml", "domain/vm-server-app.xml"
        };
    }

    @Test
    public void requestResponse() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContext(0).getRegistry().get("clientFlow");
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContext(0)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("hello world"));
    }

    @Test
    public void requestReply() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContext(0).getRegistry().get("clientFlowRequestReply");
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContext(0)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("hello world"));
    }

    @Test
    public void oneWay() throws Exception
    {
        //TODO remove reference to AbstractMuleTestCase
        Flow clientFlow = getMuleContext(0).getRegistry().get("clientFlow");
        MuleEvent response = clientFlow.process(new DefaultMuleEvent(new DefaultMuleMessage("test-data", (Map<String, Object>) null, getMuleContext(0)), MessageExchangePattern.REQUEST_RESPONSE, clientFlow));
        assertThat(response.getMessageAsString(), Is.is("test-data"));
    }

}

/*
 * $Id$
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collection;

import javax.activation.DataHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.nio.tcp.NioProperty;
import org.mule.transport.nio.tcp.NioTest;
import org.mule.util.IOUtils;
import org.mule.util.StringDataSource;

@NioTest
public class HttpAttachmentsFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    @Rule
    public NioProperty nio = new NioProperty(true);
    
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    public HttpAttachmentsFunctionalTestCase(final ConfigVariant variant, final String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "nio/http-attachments-functional-test-service.xml"},
            {ConfigVariant.FLOW, "nio/http-attachments-functional-test-flow.xml"}});
    }

    @Test
    public void testSendAttachment() throws Exception
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component)
                throws Exception
            {
                assertEquals("application/octet-stream; charset=ISO-8859-1", context.getMessage()
                    .getInboundProperty(MuleProperties.CONTENT_TYPE_PROPERTY));
                assertEquals("We should have an attachment", 1, context.getMessage()
                    .getInboundAttachmentNames()
                    .size());
                final DataHandler dh = context.getMessage().getInboundAttachment("attach1");
                assertNotNull("DataHandler with name 'attach1' should not be null", dh);
                assertEquals("We should have an attachment with foo", "foo",
                    IOUtils.toString(dh.getInputStream()));
                assertEquals("text/plain; charset=ISO-8859-1", dh.getContentType());
            }
        });

        final LocalMuleClient client = muleContext.getClient();
        final DefaultMuleMessage msg = new DefaultMuleMessage("test", muleContext);
        msg.setEncoding("ISO-8859-1");
        msg.addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));

        final MuleMessage result = client.send("endpoint1", msg);
        assertEquals("test Well Received!", result.getPayloadAsString());

        assertEquals("We should have no attachments coming back", 0, result.getInboundAttachmentNames()
            .size());
    }

    @Test
    public void testReceiveAttachment() throws Exception
    {
        final FunctionalTestComponent ftc = getFunctionalTestComponent("testComponent");
        assertNotNull(ftc);
        ftc.setEventCallback(new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component)
                throws Exception
            {
                context.getMessage().addOutboundAttachment("attach1",
                    new DataHandler(new StringDataSource("foo", "attach1")));
            }
        });

        final LocalMuleClient client = muleContext.getClient();
        final MuleMessage msg = new DefaultMuleMessage("test", muleContext);

        msg.addOutboundAttachment("attach1", new DataHandler(new StringDataSource("foo", "attach1")));

        final MuleMessage result = client.send("endpoint1", msg);
        assertEquals("test Well Received!", result.getPayloadAsString());
        assertEquals("We should have 1 attachments coming back", 1, result.getInboundAttachmentNames().size());
        assertEquals("There should be no outbound attachments", 0, result.getOutboundAttachmentNames().size());
        final DataHandler dh = result.getInboundAttachment("attach1");
        assertNotNull("DataHandler with name 'attach1' should not be null", dh);
        assertEquals("We should have an attachment with foo", "foo", IOUtils.toString(dh.getInputStream()));
        assertEquals("text/plain; charset=UTF-8", dh.getContentType());
    }
}

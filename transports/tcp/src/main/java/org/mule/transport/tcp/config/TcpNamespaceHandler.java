/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.ClassOrRefDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.tcp.PollingTcpConnector;
import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.tcp.TcpProtocol;
import org.mule.transport.tcp.protocols.CustomClassLoadingLengthProtocol;
import org.mule.transport.tcp.protocols.DirectProtocol;
import org.mule.transport.tcp.protocols.EOFProtocol;
import org.mule.transport.tcp.protocols.LengthProtocol;
import org.mule.transport.tcp.protocols.MuleMessageDirectProtocol;
import org.mule.transport.tcp.protocols.MuleMessageEOFProtocol;
import org.mule.transport.tcp.protocols.MuleMessageLengthProtocol;
import org.mule.transport.tcp.protocols.MuleMessageSafeProtocol;
import org.mule.transport.tcp.protocols.SafeProtocol;
import org.mule.transport.tcp.protocols.StreamingProtocol;
import org.mule.transport.tcp.protocols.XmlMessageEOFProtocol;
import org.mule.transport.tcp.protocols.XmlMessageProtocol;
import org.mule.util.SpiTransposer;
import org.mule.util.SpiUtils;

/**
 * Registers a Bean Definition Parser for handling <code><tcp:connector></code> elements.
 */
public class TcpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    private static final String TCP_PROTOCOL_PROPERTY = "tcpProtocol";

    public void init()
    {
        SpiUtils.registerTransposer(new SpiTransposer(){
            @Override
            public boolean isNameTransposible(String name)
            {
                return "tcp".equalsIgnoreCase(name);
            }

            @Override
            public String transposeName(String name)
            {
                if (isNioEnabled())
                {
                    return "niotcp";
                }
                return name;
            }

            protected boolean isNioEnabled()
            {
//                String isNioEnabledStr = muleContext.getRegistry().get(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
                return //Boolean.parseBoolean(isNioEnabledStr) || 
                                Boolean.getBoolean(MuleProperties.NIO_TRANSPORT_ENABLED_PROPERTY);
            }
        });
        
        registerStandardTransportEndpoints(TcpConnector.TCP, URIBuilder.SOCKET_ATTRIBUTES);

        registerConnectorDefinitionParser(new NioSelectorDelegatingDefinitionParser(
                new MuleOrphanDefinitionParser(org.mule.transport.nio.tcp.TcpConnector.class, true), 
                new MuleOrphanDefinitionParser(TcpConnector.class, true)));

        registerBeanDefinitionParser("polling-connector", new NioSelectorDelegatingDefinitionParser(
            new MuleOrphanDefinitionParser(org.mule.transport.nio.tcp.PollingTcpConnector.class, true),
            new MuleOrphanDefinitionParser(PollingTcpConnector.class, true)));
        
        registerBeanDefinitionParser("custom-protocol", new NioSelectorDelegatingDefinitionParser(
            new ChildDefinitionParser("tcpProtocol", null, org.mule.transport.nio.tcp.TcpProtocol.class, true),
            new ChildDefinitionParser("tcpProtocol", null, TcpProtocol.class, true)));
        
        registerBeanDefinitionParser("xml-protocol", new NioSelectorDelegatingDefinitionParser(
            new ChildDefinitionParser("tcpProtocol", org.mule.transport.nio.tcp.protocols.XmlMessageProtocol.class),
            new ChildDefinitionParser("tcpProtocol", XmlMessageProtocol.class)));
        
        registerBeanDefinitionParser("xml-eof-protocol", new NioSelectorDelegatingDefinitionParser(
            new ChildDefinitionParser("tcpProtocol", org.mule.transport.nio.tcp.protocols.XmlMessageEOFProtocol.class),
            new ChildDefinitionParser("tcpProtocol", XmlMessageEOFProtocol.class)));
        
        registerBeanDefinitionParser("safe-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.SafeProtocol.class, org.mule.transport.nio.tcp.protocols.MuleMessageSafeProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(SafeProtocol.class, MuleMessageSafeProtocol.class)));
        
        registerBeanDefinitionParser("length-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.LengthProtocol.class, org.mule.transport.nio.tcp.protocols.MuleMessageLengthProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(LengthProtocol.class, MuleMessageLengthProtocol.class)));
        
        registerBeanDefinitionParser("eof-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.EOFProtocol.class, org.mule.transport.nio.tcp.protocols.MuleMessageEOFProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(EOFProtocol.class, MuleMessageEOFProtocol.class)));
        
        registerBeanDefinitionParser("direct-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.DirectProtocol.class, org.mule.transport.nio.tcp.protocols.MuleMessageDirectProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(DirectProtocol.class, MuleMessageDirectProtocol.class)));
        
        registerBeanDefinitionParser("streaming-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.StreamingProtocol.class, org.mule.transport.nio.tcp.protocols.MuleMessageDirectProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(StreamingProtocol.class, MuleMessageDirectProtocol.class)));
        
        registerBeanDefinitionParser("custom-protocol", new ClassOrRefDefinitionParser(TCP_PROTOCOL_PROPERTY));
        
        registerBeanDefinitionParser("custom-class-loading-protocol", new NioSelectorDelegatingDefinitionParser(
            new ByteOrMessageProtocolDefinitionParser(org.mule.transport.nio.tcp.protocols.CustomClassLoadingLengthProtocol.class, org.mule.transport.nio.tcp.protocols.CustomClassLoadingLengthProtocol.class),
            new ByteOrMessageProtocolDefinitionParser(CustomClassLoadingLengthProtocol.class, CustomClassLoadingLengthProtocol.class)));
    }

}

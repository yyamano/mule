/*
 * $Id: HttpNamespaceHandler.java 24139 2012-03-22 21:25:08Z evangelinamrm $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.config;



import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.processors.CheckExclusiveAttributes;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.EndpointPropertyElementDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.http.CacheControlHeader;
import org.mule.transport.http.CookieWrapper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpPollingConnector;
import org.mule.transport.http.builder.HttpCookiesDefinitionParser;
import org.mule.transport.http.builder.HttpResponseDefinitionParser;
import org.mule.transport.http.components.HttpResponseBuilder;
import org.mule.transport.http.components.RestServiceWrapper;
import org.mule.transport.http.components.StaticResourceMessageProcessor;
import org.mule.transport.http.filters.HttpBasicAuthenticationFilter;
import org.mule.transport.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.http.transformers.HttpClientMethodResponseToObject;
import org.mule.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.transport.http.transformers.HttpResponseToString;
import org.mule.transport.http.transformers.MuleMessageToHttpResponse;
import org.mule.transport.http.transformers.ObjectToHttpClientMethodRequest;
import org.mule.transport.nio.http.WebSocketListeningConnector;
import org.mule.transport.nio.http.WebSocketWriter;
import org.mule.transport.nio.http.config.WebSocketEndpointConfiguration;
import org.mule.transport.tcp.config.NioSelectorDelegatingDefinitionParser;
import org.mule.util.SpiUtils;

/**
 * Reigsters a Bean Definition Parser for handling <code><http:connector></code> elements.
 */
public class HttpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        SpiUtils.registerTransposer(HttpSpiTransposer.getInstance());
        
        registerStandardTransportEndpoints(HttpConnector.HTTP, URIBuilder.SOCKET_ATTRIBUTES)
            .addAlias("contentType", HttpConstants.HEADER_CONTENT_TYPE)
            .addAlias("method", HttpConnector.HTTP_METHOD_PROPERTY);
        
        registerConnectorDefinitionParser(new NioSelectorDelegatingDefinitionParser(
            new MuleOrphanDefinitionParser(org.mule.transport.nio.http.HttpConnector.class, true),
            new MuleOrphanDefinitionParser(HttpConnector.class, true)));
        registerBeanDefinitionParser("polling-connector", new NioSelectorDelegatingDefinitionParser(
            new MuleOrphanDefinitionParser(org.mule.transport.nio.http.HttpPollingConnector.class, true),
            new MuleOrphanDefinitionParser(HttpPollingConnector.class, true)));

        registerBeanDefinitionParser("rest-service-component", new ComponentDefinitionParser(RestServiceWrapper.class));
        registerBeanDefinitionParser("payloadParameterName", new ChildListEntryDefinitionParser("payloadParameterNames", ChildMapEntryDefinitionParser.VALUE));
        registerBeanDefinitionParser("requiredParameter", new ChildMapEntryDefinitionParser("requiredParams"));
        registerBeanDefinitionParser("optionalParameter", new ChildMapEntryDefinitionParser("optionalParams"));
        
        registerBeanDefinitionParser("http-response-to-object-transformer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.transformers.HttpResponseToObject.class),
            new MessageProcessorDefinitionParser(HttpClientMethodResponseToObject.class)));
        registerBeanDefinitionParser("http-response-to-string-transformer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.transformers.HttpResponseToString.class),
            new MessageProcessorDefinitionParser(HttpResponseToString.class)));
        registerBeanDefinitionParser("object-to-http-request-transformer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.transformers.ObjectToHttpRequest.class),
            new MessageProcessorDefinitionParser(ObjectToHttpClientMethodRequest.class)));
        registerBeanDefinitionParser("message-to-http-response-transformer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.transformers.ObjectToHttpResponse.class),
            new MessageProcessorDefinitionParser(MuleMessageToHttpResponse.class)));
        registerBeanDefinitionParser("object-to-http-response-transformer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.transformers.ObjectToHttpResponse.class),
            new MessageProcessorDefinitionParser(MuleMessageToHttpResponse.class)));
        registerBeanDefinitionParser("body-to-parameter-map-transformer", new MessageProcessorDefinitionParser(HttpRequestBodyToParamMap.class));

        registerBeanDefinitionParser("error-filter", new ParentDefinitionParser());
        registerBeanDefinitionParser("request-wildcard-filter", new NioSelectorDelegatingDefinitionParser(
            new FilterDefinitionParser(org.mule.transport.nio.http.filters.HttpRequestWildcardFilter.class),
            new FilterDefinitionParser(HttpRequestWildcardFilter.class)));
        registerBeanDefinitionParser("basic-security-filter", new NioSelectorDelegatingDefinitionParser(
            new SecurityFilterDefinitionParser(org.mule.transport.nio.http.filters.HttpBasicAuthenticationFilter.class),
            new SecurityFilterDefinitionParser(HttpBasicAuthenticationFilter.class)));

        registerMuleBeanDefinitionParser("static-resource-handler", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.components.StaticResourceMessageProcessor.class),
            new MessageProcessorDefinitionParser(StaticResourceMessageProcessor.class)));

        registerBeanDefinitionParser("response-builder", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(org.mule.transport.nio.http.components.HttpResponseBuilder.class),
            new MessageProcessorDefinitionParser(HttpResponseBuilder.class)));
        registerMuleBeanDefinitionParser("header", new ChildMapEntryDefinitionParser("headers", "name", "value")).addCollection("headers");
        registerMuleBeanDefinitionParser("set-cookie", new NioSelectorDelegatingDefinitionParser(
            new HttpCookiesDefinitionParser("cookie", org.mule.transport.nio.http.CookieWrapper.class),
            new HttpCookiesDefinitionParser("cookie", CookieWrapper.class)))
            .registerPreProcessor(new CheckExclusiveAttributes(new String[][] {new String[] {"maxAge"}, new String[] {"expiryDate"}}));
        registerMuleBeanDefinitionParser("body", new TextDefinitionParser("body"));
        registerMuleBeanDefinitionParser("location", new HttpResponseDefinitionParser("header"));
        registerMuleBeanDefinitionParser("cache-control", new NioSelectorDelegatingDefinitionParser(
            new ChildDefinitionParser("cacheControl", org.mule.transport.nio.http.CacheControlHeader.class),
            new ChildDefinitionParser("cacheControl", CacheControlHeader.class)));
        registerMuleBeanDefinitionParser("expires", new HttpResponseDefinitionParser("header"));

        registerBeanDefinitionParser("websocket-listening-connector", new NioSelectorDelegatingDefinitionParser(
            new MuleOrphanDefinitionParser(WebSocketListeningConnector.class, true),
            new RuntimeExceptionDefinitionParser(new IllegalStateException("websocket-listening-connector can only be used with NIO Enabled and NIO is not enabled."))));
        registerMuleBeanDefinitionParser("websocket", new NioSelectorDelegatingDefinitionParser(
            new EndpointPropertyElementDefinitionParser(org.mule.transport.nio.http.HttpConnector.PROPERTY_WEBSOCKET_CONFIG, WebSocketEndpointConfiguration.class),
            new RuntimeExceptionDefinitionParser(new IllegalStateException("websocket can only be used with NIO Enabled and NIO is not enabled."))));
        registerMuleBeanDefinitionParser("websocket-writer", new NioSelectorDelegatingDefinitionParser(
            new MessageProcessorDefinitionParser(WebSocketWriter.class),
            new RuntimeExceptionDefinitionParser(new IllegalStateException("websocket-writer can only be used with NIO Enabled and NIO is not enabled."))));
    }
}

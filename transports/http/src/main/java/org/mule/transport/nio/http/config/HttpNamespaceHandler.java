/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http.config;

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
import org.mule.transport.http.transformers.HttpRequestBodyToParamMap;
import org.mule.transport.nio.http.CacheControlHeader;
import org.mule.transport.nio.http.CookieWrapper;
import org.mule.transport.nio.http.HttpConnector;
import org.mule.transport.nio.http.HttpConstants;
import org.mule.transport.nio.http.HttpPollingConnector;
import org.mule.transport.nio.http.WebSocketListeningConnector;
import org.mule.transport.nio.http.WebSocketWriter;
import org.mule.transport.nio.http.components.HttpResponseBuilder;
import org.mule.transport.nio.http.components.StaticResourceMessageProcessor;
import org.mule.transport.nio.http.filters.HttpBasicAuthenticationFilter;
import org.mule.transport.nio.http.filters.HttpRequestWildcardFilter;
import org.mule.transport.nio.http.transformers.HttpResponseToObject;
import org.mule.transport.nio.http.transformers.HttpResponseToString;
import org.mule.transport.nio.http.transformers.ObjectToHttpRequest;
import org.mule.transport.nio.http.transformers.ObjectToHttpResponse;

/**
 * Registers a Bean Definition Parser for handling <code><http:connector></code>
 * elements.
 */
public class HttpNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
//        registerStandardTransportEndpoints(HttpConnector.HTTP, URIBuilder.SOCKET_ATTRIBUTES).addAlias(
//            "contentType", HttpConstants.HEADER_CONTENT_TYPE).addAlias("method",
//            HttpConnector.HTTP_METHOD_PROPERTY);
//
//        registerConnectorDefinitionParser(HttpConnector.class);
//
//        registerBeanDefinitionParser("polling-connector", new MuleOrphanDefinitionParser(
//            HttpPollingConnector.class, true));
//
//        registerBeanDefinitionParser("rest-service-component", new ComponentDefinitionParser(
//            RestServiceWrapper.class));
//        registerBeanDefinitionParser("payloadParameterName", new ChildListEntryDefinitionParser(
//            "payloadParameterNames", ChildMapEntryDefinitionParser.VALUE));
//        registerBeanDefinitionParser("requiredParameter", new ChildMapEntryDefinitionParser("requiredParams"));
//        registerBeanDefinitionParser("optionalParameter", new ChildMapEntryDefinitionParser("optionalParams"));
//
//        registerBeanDefinitionParser("http-response-to-object-transformer",
//            new MessageProcessorDefinitionParser(HttpResponseToObject.class));
//        registerBeanDefinitionParser("http-response-to-string-transformer",
//            new MessageProcessorDefinitionParser(HttpResponseToString.class));
//        registerBeanDefinitionParser("object-to-http-request-transformer",
//            new MessageProcessorDefinitionParser(ObjectToHttpRequest.class));
//        registerBeanDefinitionParser("message-to-http-response-transformer",
//            new MessageProcessorDefinitionParser(ObjectToHttpResponse.class));
//        registerBeanDefinitionParser("object-to-http-response-transformer",
//            new MessageProcessorDefinitionParser(ObjectToHttpResponse.class));
//        registerBeanDefinitionParser("body-to-parameter-map-transformer",
//            new MessageProcessorDefinitionParser(HttpRequestBodyToParamMap.class));
//
//        registerBeanDefinitionParser("error-filter", new ParentDefinitionParser());
//        registerBeanDefinitionParser("request-wildcard-filter", new FilterDefinitionParser(
//            HttpRequestWildcardFilter.class));
//        registerBeanDefinitionParser("basic-security-filter", new SecurityFilterDefinitionParser(
//            HttpBasicAuthenticationFilter.class));
//
//        registerMuleBeanDefinitionParser("static-resource-handler", new MessageProcessorDefinitionParser(
//            StaticResourceMessageProcessor.class));
//
//        registerBeanDefinitionParser("response-builder", new MessageProcessorDefinitionParser(
//            HttpResponseBuilder.class));
//        registerMuleBeanDefinitionParser("header",
//            new ChildMapEntryDefinitionParser("headers", "name", "value")).addCollection("headers");
//        registerMuleBeanDefinitionParser("set-cookie",
//            new HttpCookiesDefinitionParser("cookie", CookieWrapper.class)).registerPreProcessor(
//            new CheckExclusiveAttributes(new String[][]{new String[]{"maxAge"}, new String[]{"expiryDate"}}));
//        registerMuleBeanDefinitionParser("body", new TextDefinitionParser("body"));
//        registerMuleBeanDefinitionParser("location", new HttpResponseDefinitionParser("header"));
//        registerMuleBeanDefinitionParser("cache-control", new ChildDefinitionParser("cacheControl",
//            CacheControlHeader.class));
//        registerMuleBeanDefinitionParser("expires", new HttpResponseDefinitionParser("header"));
//
//        registerBeanDefinitionParser("websocket-listening-connector", new MuleOrphanDefinitionParser(
//            WebSocketListeningConnector.class, true));
//        registerMuleBeanDefinitionParser("websocket", new EndpointPropertyElementDefinitionParser(
//            HttpConnector.PROPERTY_WEBSOCKET_CONFIG, WebSocketEndpointConfiguration.class));
//        registerMuleBeanDefinitionParser("websocket-writer", new MessageProcessorDefinitionParser(
//            WebSocketWriter.class));
    }
}

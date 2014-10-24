/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.config;


import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.GenericChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.oauth2.AuthenticationCodeAuthenticate;
import org.mule.module.oauth2.AuthorizationCodeConfig;
import org.mule.module.oauth2.AuthorizationRequestHandler;
import org.mule.module.oauth2.ParameterExtractor;
import org.mule.module.oauth2.TokenRequestHandler;
import org.mule.module.oauth2.TokenResponseConfiguration;

public class OAuthNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        final MuleOrphanDefinitionParser authenticationCodeParser = new MuleOrphanDefinitionParser(AuthorizationCodeConfig.class, true);
        authenticationCodeParser.addReference("requestConfig");
        authenticationCodeParser.addReference("listenerConfig");
        registerMuleBeanDefinitionParser("authentication-code", authenticationCodeParser);
        registerMuleBeanDefinitionParser("authorization-request", new ChildDefinitionParser("authorizationRequestHandler", AuthorizationRequestHandler.class));
        registerMuleBeanDefinitionParser("token-request", new ChildDefinitionParser("tokenRequestHandler", TokenRequestHandler.class));
        registerMuleBeanDefinitionParser("token-response", new ChildDefinitionParser("tokenResponseConfiguration", TokenResponseConfiguration.class));
        registerMuleBeanDefinitionParser("custom-parameters", new GenericChildMapDefinitionParser("customParameters", "custom-parameter", "paramName", "value"));
        registerMuleBeanDefinitionParser("custom-parameter-extractor", new ChildDefinitionParser("parameterExtractor", ParameterExtractor.class));
        final ChildDefinitionParser authenticationDefinitionParser = new ChildDefinitionParser("auth", AuthenticationCodeAuthenticate.class);
        authenticationDefinitionParser.addReference("config");
        registerMuleBeanDefinitionParser("authentication-code-auth", authenticationDefinitionParser);
    }

}

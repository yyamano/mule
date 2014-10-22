/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.oauth.config;


import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.GenericChildMapDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;

public class OAuthNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerMuleBeanDefinitionParser("authentication-code", new MuleOrphanDefinitionParser(AuthorizationCodeGrantTypeConfig.class, true));
        registerMuleBeanDefinitionParser("authorization-request", new ChildDefinitionParser("authorizationRequest", AuthorizationRequest.class));
        registerMuleBeanDefinitionParser("token-request", new ChildDefinitionParser("tokenRequest", TokenRequest.class));
        registerMuleBeanDefinitionParser("token-response", new ChildDefinitionParser("tokenResponse", TokenResponse.class));
        registerMuleBeanDefinitionParser("custom-parameters", new GenericChildMapDefinitionParser("customParameters", "custom-parameter", "paramName", "value"));
        registerMuleBeanDefinitionParser("custom-parameter-extractor", new ChildDefinitionParser("parameterExtractor", ParameterExtractor.class));
        final ChildDefinitionParser authenticationDefinitionParser = new ChildDefinitionParser("auth", AuthenticationCodeAuthenticate.class);
        authenticationDefinitionParser.addReference("config");
        registerMuleBeanDefinitionParser("authentication-code-auth", authenticationDefinitionParser);
    }

}

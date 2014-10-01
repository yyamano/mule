/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth.config;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.module.oauth.http.HttpEndpointListener;
import org.mule.security.oauth.OAuthUtils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthenticationCodeGrantType implements Startable
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private String clientId;
    private String clientSecret;
    private String localAuthorizationUrl;
    private String authorizationUrl;
    private String redirectionUrl;
    private String tokenUrl;
    private String scope;
    private String state;
    private Map<String, String> customParameters = new HashMap<String, String>();
    private ParameterExtractor parameterExtractor = new ParameterExtractor();

    public void setName(String name)
    {
        this.name = name;
    }

    public void setClientId(String clientId)
    {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
    }

    public void setLocalAuthorizationUrl(String localAuthorizationUrl)
    {
        this.localAuthorizationUrl = localAuthorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
    }

    public void setRedirectionUrl(String redirectionUrl)
    {
        this.redirectionUrl = redirectionUrl;
    }

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    public void setScope(String scope)
    {
        this.scope = scope;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public Map<String, String> getCustomParameters()
    {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters)
    {
        this.customParameters = customParameters;
    }

    public void setParameterExtractor(ParameterExtractor parameterExtractor)
    {
        this.parameterExtractor = parameterExtractor;
    }

    @Override
    public void start() throws MuleException
    {
        createHttpServerForRedirectUrl();
    }

    private void createHttpServerForRedirectUrl()
    {
        new HttpEndpointListener(localAuthorizationUrl,
                        OAuthUtils.buildAuthorizeUrl(clientId, authorizationUrl, redirectionUrl, scope, state, customParameters),
                        redirectionUrl, new HttpEndpointListener.RequestCallback()
        {
            @Override
            public void process(String requestBody)
            {
                try
                {
                    String requestUrl = "http://localhost" + requestBody;
                    final String authenticationCode = OAuthUtils.extractAuthorizationCode(requestUrl);
                    final String state = OAuthUtils.extractState(requestUrl);
                    logger.error("state acquired: " + state);
                    //TODO retrieve state as well.
                    final String tokenUrlCallResponse = OAuthUtils.callTokenUrl(tokenUrl, OAuthUtils.buildTokenBody(clientId, clientSecret, redirectionUrl, authenticationCode));
                    final String accessToken = OAuthUtils.extractAccessToken(tokenUrlCallResponse);
                    final String refreshToken = OAuthUtils.extractRefreshToken(tokenUrlCallResponse);
                    final String expirationTime = OAuthUtils.extractExpirationTime(tokenUrlCallResponse);
                    logger.error("Access Token acquired: " + accessToken);
                    logger.error("Refresh Token acquired: " + refreshToken);
                    logger.error("Expires in acquired: " + expirationTime);
                }
                catch (Exception e)
                {
                    logger.warn("Failure trying to extract the authentication code from a authorization server call ", e);
                }
            }
        });
    }
}

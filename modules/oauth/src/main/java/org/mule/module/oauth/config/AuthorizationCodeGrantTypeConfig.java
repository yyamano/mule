/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth.config;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import org.mule.module.oauth.state.UserOAuthState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationCodeGrantTypeConfig implements Startable, AuthorizationCodeGrantConfig
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String name;
    private String clientId;
    private String clientSecret;
    private String redirectionUrl;
    private String oauthStateId;
    private AuthorizationRequest authorizationRequest;
    private TokenRequest tokenRequest;
    private String userId = UserOAuthState.DEFAULT_USER_ID;

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

    public void setRedirectionUrl(String redirectionUrl)
    {
        this.redirectionUrl = redirectionUrl;
    }

    public void setAuthorizationRequest(AuthorizationRequest authorizationRequest)
    {
        this.authorizationRequest = authorizationRequest;
    }

    public void setTokenRequest(TokenRequest tokenRequest)
    {
        this.tokenRequest = tokenRequest;
    }

    @Override
    public void start() throws MuleException
    {
        if (authorizationRequest != null)
        {
            authorizationRequest.setOauthConfig(this);
            authorizationRequest.startListener();
        }
        if (tokenRequest != null)
        {
            tokenRequest.setOauthConfig(this);
            tokenRequest.startListener();
        }
    }

    public String getRedirectionUrl()
    {
        return redirectionUrl;
    }

    @Override
    public String getRefreshTokenWhen()
    {
        return tokenRequest.getRefreshTokenWhen();
    }

    @Override
    public String getOAuthStateId()
    {
        return oauthStateId;
    }

    @Override
    public void refreshToken(final MuleEvent currentFlowEvent, final String userId)
    {
        tokenRequest.refreshToken(currentFlowEvent, userId);
    }

    @Override
    public String getConfigName()
    {
        return name;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    public String getClientId()
    {
        return clientId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setOauthStateId(String oauthStateId)
    {
        this.oauthStateId = oauthStateId;
    }
}

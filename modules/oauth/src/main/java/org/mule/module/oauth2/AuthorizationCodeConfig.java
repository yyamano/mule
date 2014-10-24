/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.registry.RegistrationException;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.listener.HttpListenerConfig;
import org.mule.module.oauth2.state.ConfigOAuthState;
import org.mule.module.oauth2.state.OAuthStateRegistry;

/**
 * Represents the config element for oauth:authentication-code-config.
 *
 * This config will:
 *  - If the authorization-request is defined then it will create a flow listening for an user call to begin the oauth login.
 *  - If the token-request is defined then it will create a flow for listening in the redirect uri so we can get the authentication code and retrieve the access token
 */
public class AuthorizationCodeConfig implements Initialisable, Disposable, Startable, AuthorizationCodeGrantType, MuleContextAware
{

    private String name;
    private String clientId;
    private String clientSecret;
    private String redirectionUrl;
    private String oauthStateId;
    private HttpRequestConfig requestConfig;
    private HttpListenerConfig listenerConfig;
    private AuthorizationRequestHandler authorizationRequestHandler;
    private TokenRequestHandler tokenRequestHandler;
    private ConfigOAuthState configOAuthState = new ConfigOAuthState();
    private MuleContext muleContext;
    private OAuthStateRegistry oauthStateRegistry;

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

    public void setAuthorizationRequestHandler(AuthorizationRequestHandler authorizationRequestHandler)
    {
        this.authorizationRequestHandler = authorizationRequestHandler;
    }

    public void setTokenRequestHandler(TokenRequestHandler tokenRequestHandler)
    {
        this.tokenRequestHandler = tokenRequestHandler;
    }

    public void setRequestConfig(HttpRequestConfig requestConfig)
    {
        this.requestConfig = requestConfig;
    }

    public void setListenerConfig(HttpListenerConfig listenerConfig)
    {
        this.listenerConfig = listenerConfig;
    }

    @Override
    public void start() throws MuleException
    {
        if (authorizationRequestHandler != null)
        {
            authorizationRequestHandler.setOauthConfig(this);
            authorizationRequestHandler.startListener();
        }
        if (tokenRequestHandler != null)
        {
            tokenRequestHandler.setOauthConfig(this);
            tokenRequestHandler.startListener();
        }
    }

    public String getRedirectionUrl()
    {
        return redirectionUrl;
    }

    @Override
    public String getRefreshTokenWhen()
    {
        return tokenRequestHandler.getRefreshTokenWhen();
    }

    @Override
    public String getOAuthStateId()
    {
        return oauthStateId;
    }

    @Override
    public HttpRequestConfig getRequestConfig()
    {
        return requestConfig;
    }

    @Override
    public HttpListenerConfig getListenerConfig()
    {
        return listenerConfig;
    }

    @Override
    public void refreshToken(final MuleEvent currentFlowEvent, final String oauthStateId)
    {
        tokenRequestHandler.refreshToken(currentFlowEvent, oauthStateId);
    }

    @Override
    public ConfigOAuthState getOAuthState()
    {
        return configOAuthState;
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

    public void setOauthStateId(String oauthStateId)
    {
        this.oauthStateId = oauthStateId;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            oauthStateRegistry = this.muleContext.getRegistry().lookupObject(OAuthStateRegistry.class);
            oauthStateRegistry.registerOAuthState(getConfigName(), configOAuthState);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void dispose()
    {
        oauthStateRegistry.unregisterOAuthState(getConfigName());
    }
}

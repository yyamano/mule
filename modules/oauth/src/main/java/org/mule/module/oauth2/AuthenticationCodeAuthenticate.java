/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.request.HttpAuth;
import org.mule.module.oauth2.state.OAuthStateRegistry;
import org.mule.module.oauth2.state.UserOAuthState;
import org.mule.security.oauth.OAuthUtils;
import org.mule.util.AttributeEvaluator;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.Authentication;

public class AuthenticationCodeAuthenticate implements HttpAuth, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private AuthorizationCodeGrantType config;
    private AttributeEvaluator oauthStateIdEvaluator;
    private OAuthStateRegistry oauthStateRegistry;

    @Override
    public void authenticate(MuleEvent muleEvent)
    {
        final String accessToken = oauthStateRegistry.getStateForConfig(config.getConfigName()).getStateForUser(oauthStateIdEvaluator.resolveStringValue(muleEvent)).getAccessToken();
        muleEvent.getMessage().setOutboundProperty(HttpHeaders.Names.AUTHORIZATION, OAuthUtils.buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public Authentication buildAuthentication()
    {
        //TODO remove once we fix the HttpAuth interface in the new http module.
        return null;
    }

    @Override
    public boolean shouldRetry(MuleEvent firstAttemptResponseEvent)
    {
        final String refreshTokenWhen = config.getRefreshTokenWhen();
        if (!StringUtils.isBlank(refreshTokenWhen))
        {
            final Boolean shouldRetryRequest = muleContext.getExpressionLanguage().evaluate(refreshTokenWhen, firstAttemptResponseEvent);
            if (shouldRetryRequest)
            {
                config.refreshToken(firstAttemptResponseEvent, oauthStateIdEvaluator.resolveStringValue(firstAttemptResponseEvent));
            }
            return shouldRetryRequest;
        }
        return false;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void setConfig(AuthorizationCodeConfig config)
    {
        this.config = config;
    }

    public void setOauthStateId(String oauthStateId)
    {
        oauthStateIdEvaluator = new AttributeEvaluator(oauthStateId);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            if (oauthStateIdEvaluator == null)
            {
                oauthStateIdEvaluator = new AttributeEvaluator(UserOAuthState.DEFAULT_USER_ID);
            }
            oauthStateIdEvaluator.initialize(muleContext.getExpressionManager());
            oauthStateRegistry = muleContext.getRegistry().lookupObject(OAuthStateRegistry.class);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}

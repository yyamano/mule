/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2;

import org.mule.DefaultMuleEvent;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.HttpRequester;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.http.listener.MessageProperties;
import org.mule.module.oauth2.state.UserOAuthState;
import org.mule.security.oauth.OAuthConstants;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents the Token request and response handling behaviour of the OAuth 2.0 dance. It provides support for
 * standard OAuth server implementations of the token acquisition part plus a couple of configuration attributes to
 * customize behaviour.
 */
public class TokenRequestHandler implements MuleContextAware
{

    public static final String OAUTH_STATE_PARAMETER_PREFIX = ":oauthStateId";

    protected Logger logger = LoggerFactory.getLogger(getClass());
    private String tokenUrl;
    private String refreshTokenWhen = "#[message.inboundProperties['http.status'] == 401 || message.inboundProperties['http.status'] == 403]";
    private TokenResponseConfiguration tokenResponseConfiguration = new TokenResponseConfiguration();
    private MuleContext muleContext;
    private HttpListener redirectUrlListener;
    private URL parsedTokenUrl;
    private AuthorizationCodeConfig oauthConfig;

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public void setTokenResponseConfiguration(TokenResponseConfiguration tokenResponseConfiguration)
    {
        this.tokenResponseConfiguration = tokenResponseConfiguration;
    }

    /**
     * Starts the http listener for the redirect url callback. This will create a flow with an endpoint on the
     * provided OAuth redirect uri parameter. The OAuth Server will call this url to provide the authentication code
     * required to get the access token.
     *
     * @throws MuleException if the listener couldn't be created.
     */
    public void startListener() throws MuleException
    {
        try
        {
            this.parsedTokenUrl = new URL(tokenUrl);
        }
        catch (MalformedURLException e)
        {
            logger.warn("Could not parse provided url %s. Validate that the url is correct", tokenUrl);
            throw new DefaultMuleException(e);
        }
        try
        {
            this.redirectUrlListener = new HttpListenerBuilder(muleContext)
                    .setUrl(oauthConfig.getRedirectionUrl())
                    .setFlowConstruct(new Flow("some flow name", muleContext))
                    .setMuleContext(muleContext)
                    .setListenerConfig(oauthConfig.getListenerConfig())
                    .setListener(createRedirectUrlListener()).build();
            this.redirectUrlListener.start();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Could not parse provided url %s. Validate that the url is correct", oauthConfig.getRedirectionUrl());
            throw new DefaultMuleException(e);
        }
    }

    private MessageProcessor createRedirectUrlListener()
    {
        return new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                Map<String, String> queryParams = event.getMessage().getInboundProperty(MessageProperties.HTTP_QUERY_PARAMS);
                String authorizationCode = queryParams.get(OAuthConstants.CODE_PARAMETER);
                String state = queryParams.get(OAuthConstants.STATE_PARAMETER);
                setMapPayloadWithTokenRequestParameters(event, authorizationCode);
                final MuleEvent tokenUrlResposne = invokeTokenUrl(event);
                decodeStateAndUpdateOAuthUserState(tokenUrlResposne, state);
                event.getMessage().setPayload("Successfully retrieved access token!");
                return event;
            }
        };
    }

    private void setMapPayloadWithTokenRequestParameters(MuleEvent event, String authorizationCode)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put(OAuthConstants.CODE_PARAMETER, authorizationCode);
        formData.put(OAuthConstants.CLIENT_ID_PARAMETER, oauthConfig.getClientId());
        formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, oauthConfig.getClientSecret());
        formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE);
        formData.put(OAuthConstants.REDIRECT_URI_PARAMETER, oauthConfig.getRedirectionUrl());
        event.getMessage().setPayload(formData);
    }

    private void setMapPayloadWithRefreshTokenRequestParameters(MuleEvent event, String refreshToken)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put(OAuthConstants.REFRESH_TOKEN_PARAMETER, refreshToken);
        formData.put(OAuthConstants.CLIENT_ID_PARAMETER, oauthConfig.getClientId());
        formData.put(OAuthConstants.CLIENT_SECRET_PARAMETER, oauthConfig.getClientSecret());
        formData.put(OAuthConstants.GRANT_TYPE_PARAMETER, OAuthConstants.GRANT_TYPE_REFRESH_TOKEN);
        formData.put(OAuthConstants.REDIRECT_URI_PARAMETER, oauthConfig.getRedirectionUrl());
        event.getMessage().setPayload(formData);
    }

    private MuleEvent invokeTokenUrl(MuleEvent event) throws MuleException
    {
        final HttpRequester httpRequester = new HttpRequester();
        httpRequester.setMuleContext(muleContext);
        httpRequester.setHost(parsedTokenUrl.getHost());
        httpRequester.setPath(parsedTokenUrl.getPath());
        httpRequester.setPort(String.valueOf(parsedTokenUrl.getPort()));
        httpRequester.setMethod("POST");
        final HttpRequestConfig requestConfig = new HttpRequestConfig();
        requestConfig.setMuleContext(muleContext);
        if ("https".equals(parsedTokenUrl.getProtocol()))
        {
            requestConfig.setTlsContext(new DefaultTlsContextFactory());
        }
        requestConfig.initialise();
        httpRequester.setConfig(requestConfig);
        httpRequester.initialise();
        return httpRequester.process(event);
    }

    private void decodeStateAndUpdateOAuthUserState(final MuleEvent tokenUrlResposne, String state) throws org.mule.api.registry.RegistrationException
    {
        String oauthStateId = UserOAuthState.DEFAULT_USER_ID;
        if (state != null && state.contains(OAUTH_STATE_PARAMETER_PREFIX))
        {
            final String oauthStateUserIdParameterAssignment = OAUTH_STATE_PARAMETER_PREFIX + "=";
            final int oauthStateIdSuffixIndex = state.indexOf(oauthStateUserIdParameterAssignment);
            oauthStateId = state.substring(oauthStateIdSuffixIndex + oauthStateUserIdParameterAssignment.length(), state.length());
            state = state.substring(0, oauthStateIdSuffixIndex);
            if (state.isEmpty())
            {
                state = null;
            }
        }
        updateOAuthUserState(tokenUrlResposne, state, oauthStateId);
    }

    private void updateOAuthUserState(MuleEvent tokenUrlResponse, String state, String oauthStateId) throws RegistrationException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Update OAuth State for oauthStateId %s", oauthStateId);
        }
        final String accessToken = muleContext.getExpressionManager().parse(tokenResponseConfiguration.getAccessToken(), tokenUrlResponse);
        final String refreshToken = muleContext.getExpressionManager().parse(tokenResponseConfiguration.getRefreshToken(), tokenUrlResponse);
        final String expiresIn = muleContext.getExpressionManager().parse(tokenResponseConfiguration.getExpiresIn(), tokenUrlResponse);

        if (logger.isDebugEnabled())
        {
            logger.debug("New OAuth State for oauthStateId %s is: accessToken(%s), refreshToken(%s), expiresIn(%s), state(%s)", oauthStateId, accessToken, refreshToken, expiresIn, state);
        }

        final UserOAuthState userOAuthState = oauthConfig.getOAuthState().getStateForUser(oauthStateId);
        userOAuthState.setAccessToken(accessToken);
        userOAuthState.setRefreshToken(refreshToken);
        userOAuthState.setExpiresIn(expiresIn);

        //State may be null because there's no state or because this was called after refresh token.
        if (state != null)
        {
            userOAuthState.setState(state);
        }

        for (ParameterExtractor parameterExtractor : tokenResponseConfiguration.getParameterExtractors())
        {
            final Object parameterValue = muleContext.getExpressionManager().evaluate(parameterExtractor.getValue(), tokenUrlResponse);
            if (parameterValue != null)
            {
                userOAuthState.getTokenResponseParameters().put(parameterExtractor.getParamName(), parameterValue);
            }
        }
    }

    public String getRefreshTokenWhen()
    {
        return refreshTokenWhen;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setOauthConfig(AuthorizationCodeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    /**
     * Executes a refresh token for a particular user. It will call the OAuth Server token url
     * and provide the refresh token to get a new access token.
     *
     * @param currentEvent the event being processed when the refresh token was required.
     * @param userId the id of the user for who we need to update the access token.
     */
    public void refreshToken(final MuleEvent currentEvent, String userId)
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Executing refresh token for user " + userId);
            }
            final UserOAuthState userOAuthState = oauthConfig.getOAuthState().getStateForUser(userId);
            final MuleEvent muleEvent = DefaultMuleEvent.copy(currentEvent);
            muleEvent.getMessage().clearProperties(PropertyScope.OUTBOUND);
            final String userRefreshToken = userOAuthState.getRefreshToken();
            if (userRefreshToken == null)
            {
                throw new DefaultMuleException(CoreMessages.createStaticMessage("The user with user id %s has no refresh token in his OAuth state so we can't execute the refresh token call", userId));
            }
            setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userRefreshToken);
            final MuleEvent refreshTokenResponse = invokeTokenUrl(muleEvent);
            updateOAuthUserState(refreshTokenResponse, null, userId);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}

package org.mule.module.oauth.config;

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
import org.mule.construct.Flow;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.HttpRequester;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.http.listener.MessageProperties;
import org.mule.module.oauth.state.ContextOAuthState;
import org.mule.module.oauth.state.UserOAuthState;
import org.mule.transport.ssl.DefaultTlsContextFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenRequest implements MuleContextAware
{

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private String tokenUrl;
    private String refreshTokenWhen = "#[message.inboundProperties['http.status'] == 401 || message.inboundProperties['http.status'] == 403]";
    private TokenResponse tokenResponse = new TokenResponse();
    private MuleContext muleContext;
    private HttpListener redirectUrlListener;
    private URL parsedTokenUrl;
    private AuthorizationCodeGrantTypeConfig oauthConfig;

    public void setTokenUrl(String tokenUrl)
    {
        this.tokenUrl = tokenUrl;
    }

    public void setRefreshTokenWhen(String refreshTokenWhen)
    {
        this.refreshTokenWhen = refreshTokenWhen;
    }

    public void setTokenResponse(TokenResponse tokenResponse)
    {
        this.tokenResponse = tokenResponse;
    }

    public void startListener() throws DefaultMuleException
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
                    .setListener(createRedirectUrlListener()).build();
            this.redirectUrlListener.start();
        }
        catch (MuleException e)
        {
            throw new DefaultMuleException(e);
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
                MessageProperties messageProperties = event.getMessage().getInboundProperty("http.properties");
                String authorizationCode = messageProperties.getQueryParams().get("code");
                String state = messageProperties.getQueryParams().get("state");

                setMapPayloadWithTokenRequestParameters(event, authorizationCode);

                final MuleEvent tokenUrlResposne = invokeTokenUrl(event);
                //TODO se were to get the user id from. Probably using the state attribute?
                updateUserOAuthState(tokenUrlResposne, state);

                return event;
            }
        };
    }

    private void setMapPayloadWithTokenRequestParameters(MuleEvent event, String authorizationCode)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put("code", authorizationCode);
        formData.put("client_id", oauthConfig.getClientId());
        formData.put("client_secret", oauthConfig.getClientSecret());
        formData.put("grant_type", "authorization_code");
        formData.put("redirect_uri", oauthConfig.getRedirectionUrl());
        event.getMessage().setPayload(formData);
    }

    private void setMapPayloadWithRefreshTokenRequestParameters(MuleEvent event, String refreshToken)
    {
        final HashMap<String, String> formData = new HashMap<String, String>();
        formData.put("refresh_token", refreshToken);
        formData.put("client_id", oauthConfig.getClientId());
        formData.put("client_secret", oauthConfig.getClientSecret());
        formData.put("grant_type", "refresh_token");
        formData.put("redirect_uri", oauthConfig.getRedirectionUrl());
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

    private void updateUserOAuthState(final MuleEvent tokenUrlResposne, String state) throws org.mule.api.registry.RegistrationException
    {
        final String accessToken = muleContext.getExpressionManager().parse(tokenResponse.getAccessToken(), tokenUrlResposne);
        final String refreshToken = muleContext.getExpressionManager().parse(tokenResponse.getRefreshToken(), tokenUrlResposne);
        final String expiresIn = muleContext.getExpressionManager().parse(tokenResponse.getExpiresIn(), tokenUrlResposne);
        final UserOAuthState userOAuthState = muleContext.getRegistry().lookupObject(ContextOAuthState.class).getStateForConfig(oauthConfig.getConfigName()).getStateForUser(oauthConfig.getUserId());
        userOAuthState.setAccessToken(accessToken);
        userOAuthState.setRefreshToken(refreshToken);
        userOAuthState.setExpiresIn(expiresIn);

        if (state != null)
        {
            userOAuthState.setState(state);
        }

        for (ParameterExtractor parameterExtractor : tokenResponse.getParameterExtractors())
        {
            final Object parameterValue = muleContext.getExpressionManager().evaluate(parameterExtractor.getValue(), tokenUrlResposne);
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

    public void setOauthConfig(AuthorizationCodeGrantTypeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    public void refreshToken(final MuleEvent currentEvent, String userId)
    {
        try
        {
            final UserOAuthState userOAuthState = muleContext.getRegistry().lookupObject(ContextOAuthState.class).getStateForConfig(oauthConfig.getConfigName()).getStateForUser(userId);
            final MuleEvent muleEvent = DefaultMuleEvent.copy(currentEvent);
            muleEvent.getMessage().clearProperties(PropertyScope.OUTBOUND);
            setMapPayloadWithRefreshTokenRequestParameters(muleEvent, userOAuthState.getRefreshToken());
            final MuleEvent refreshTokenResponse = invokeTokenUrl(muleEvent);
            updateUserOAuthState(refreshTokenResponse, null);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}

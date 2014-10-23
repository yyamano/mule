package org.mule.module.oauth.config;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.request.HttpAuth;
import org.mule.module.oauth.state.ContextOAuthState;
import org.mule.module.oauth.state.UserOAuthState;
import org.mule.security.oauth.OAuthUtils;
import org.mule.util.AttributeEvaluator;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.client.api.Authentication;

public class AuthenticationCodeAuthenticate implements HttpAuth, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private AuthorizationCodeGrantConfig config;
    private String oauthStateId = UserOAuthState.DEFAULT_USER_ID;
    private AttributeEvaluator oauthStateIdEvaluator;
    private ContextOAuthState contextOAuthState;

    @Override
    public void authenticate(MuleEvent muleEvent)
    {
        final String accessToken = contextOAuthState.getStateForConfig(config.getConfigName()).getStateForUser(oauthStateIdEvaluator.resolveStringValue(muleEvent)).getAccessToken();
        muleEvent.getMessage().setOutboundProperty(HttpHeaders.Names.AUTHORIZATION, OAuthUtils.buildAuthorizationHeaderContent(accessToken));
    }

    @Override
    public Authentication buildAuthentication()
    {
        //TODO remove
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

    public void setConfig(AuthorizationCodeGrantTypeConfig config)
    {
        this.config = config;
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
            contextOAuthState = muleContext.getRegistry().lookupObject(ContextOAuthState.class);
            oauthStateIdEvaluator = new AttributeEvaluator(oauthStateId).initialize(muleContext.getExpressionManager());
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}

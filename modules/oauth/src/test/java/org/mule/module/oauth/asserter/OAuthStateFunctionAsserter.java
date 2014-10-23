package org.mule.module.oauth.asserter;

import static org.junit.Assert.assertThat;

import org.mule.api.el.ExpressionLanguage;
import org.mule.module.oauth.state.UserOAuthState;

import org.hamcrest.core.Is;

public class OAuthStateFunctionAsserter
{

    private final ExpressionLanguage expressionLanguage;
    private final String configName;
    private final String userId;

    private OAuthStateFunctionAsserter(ExpressionLanguage expressionLanguage, String configName, String userId)
    {
        this.expressionLanguage = expressionLanguage;
        this.configName = configName;
        this.userId = userId;
    }

    public static OAuthStateFunctionAsserter createFrom(ExpressionLanguage expressionLanguage, String configName)
    {
        return new OAuthStateFunctionAsserter(expressionLanguage, configName, UserOAuthState.DEFAULT_USER_ID);
    }

    public static OAuthStateFunctionAsserter createFrom(ExpressionLanguage expressionLanguage, String configName, String userId)
    {
        return new OAuthStateFunctionAsserter(expressionLanguage, configName, userId);
    }

    public OAuthStateFunctionAsserter assertAccessTokenIs(String expectedAccessToken)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s','%s').accessToken", configName, userId)), Is.<Object>is(expectedAccessToken));
        return this;
    }

    public OAuthStateFunctionAsserter assertRefreshTokenIs(String expectedRefreshToken)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s','%s').refreshToken", configName, userId)), Is.<Object>is(expectedRefreshToken));
        return this;
    }

    public OAuthStateFunctionAsserter assertState(String expectedState)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s','%s').state", configName, userId)), Is.<Object>is(expectedState));
        return this;
    }

    public OAuthStateFunctionAsserter assertExpiresInIs(String expectedExpiresIs)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s','%s').expiresIn", configName, userId)), Is.<Object>is(expectedExpiresIs));
        return this;
    }

    public OAuthStateFunctionAsserter assertContainsCustomTokenResponseParam(String paramName, String paramValue)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s','%s').tokenResponseParameters['%s']", configName, userId, paramName)), Is.<Object>is(paramValue));
        return this;
    }
}

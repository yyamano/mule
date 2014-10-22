package org.mule.module.oauth.asserter;

import static org.junit.Assert.assertThat;

import org.mule.api.el.ExpressionLanguage;
import org.mule.module.oauth.state.UserOAuthState;

import org.hamcrest.core.Is;

public class OAuthStateFunctionAsserter
{

    private final ExpressionLanguage expressionLanguage;
    private final String configName;

    private OAuthStateFunctionAsserter(ExpressionLanguage expressionLanguage, String configName)
    {
        this.expressionLanguage = expressionLanguage;
        this.configName = configName;
    }

    public static OAuthStateFunctionAsserter createFrom(ExpressionLanguage expressionLanguage, String configName)
    {
        return new OAuthStateFunctionAsserter(expressionLanguage, configName);
    }

    public static OAuthStateFunctionAsserter createFrom(ExpressionLanguage expressionLanguage)
    {
        return new OAuthStateFunctionAsserter(expressionLanguage, UserOAuthState.DEFAULT_USER_ID);
    }

    public OAuthStateFunctionAsserter assertAccessTokenIs(String expectedAccessToken)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s').accessToken", configName)), Is.<Object>is(expectedAccessToken));
        return this;
    }

    public OAuthStateFunctionAsserter assertRefreshTokenIs(String expectedRefreshToken)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s').refreshToken", configName)), Is.<Object>is(expectedRefreshToken));
        return this;
    }

    public OAuthStateFunctionAsserter assertState(String expectedState)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s').state", configName)), Is.<Object>is(expectedState));
        return this;
    }

    public OAuthStateFunctionAsserter assertExpiresInIs(String expectedExpiresIs)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s').expiresIn", configName)), Is.<Object>is(expectedExpiresIs));
        return this;
    }

    public OAuthStateFunctionAsserter assertContainsCustomTokenResponseParam(String paramName, String paramValue)
    {
        assertThat(expressionLanguage.evaluate(String.format("oauthState('%s').tokenResponseParameters['%s']", configName, paramName)), Is.<Object>is(paramValue));
        return this;
    }
}

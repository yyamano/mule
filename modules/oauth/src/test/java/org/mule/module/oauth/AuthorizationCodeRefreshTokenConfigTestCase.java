package org.mule.module.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.module.http.HttpHeaders;
import org.mule.module.oauth.state.ContextOAuthState;
import org.mule.module.oauth.state.UserOAuthState;
import org.mule.security.oauth.OAuthConstants;
import org.mule.tck.junit4.rule.SystemProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeRefreshTokenConfigTestCase extends AbstractAuthorizationCodeFunctionalTestCase
{

    private static final String RESOURCE_PATH = "/resource";
    public static final String RESOURCE_RESULT = "resource result";
    public static final String REFRESHED_ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4guasdfsdfa";

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d" + AUTHORIZE_PATH, oauthServerPort.getNumber()));
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/redirect", localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, oauthServerPort.getNumber()));


    @Override
    protected String getConfigFile()
    {
        return "authorization-code-refresh-token-config.xml";
    }

    @Test
    public void afterFailureDoRefreshTokenWithDefaultValue() throws Exception
    {
        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                                     .willReturn(aResponse()
                                                         .withBody("{" +
                                                                   "\"" + OAuthConstants.ACCESS_TOKEN_PARAMETER + "\":\"" + REFRESHED_ACCESS_TOKEN + "\"," +
                                                                   "\"" + OAuthConstants.EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
                                                                   "\"" + OAuthConstants.REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"}")));

        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(REFRESHED_ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(200)
                                                         .withBody(RESOURCE_RESULT)));
        wireMockRule.stubFor(post(urlEqualTo(RESOURCE_PATH))
                                     .withHeader(HttpHeaders.Names.AUTHORIZATION,
                                                 containing(ACCESS_TOKEN))
                                     .willReturn(aResponse()
                                                         .withStatus(403)
                                                         .withBody("")));

        final ContextOAuthState oauthState = muleContext.getRegistry().lookupObject(ContextOAuthState.class);
        final UserOAuthState userOauthState = oauthState.getStateForConfig("oauthConfig").getStateForUser(UserOAuthState.DEFAULT_USER_ID);
        userOauthState.setAccessToken(ACCESS_TOKEN);
        userOauthState.setRefreshToken(REFRESH_TOKEN);

        Flow flow = (Flow) getFlowConstruct("testFlow");
        final MuleEvent result = flow.process(getTestEvent("message"));
        assertThat(result.getMessage().getPayloadAsString(), is(RESOURCE_RESULT));

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REFRESH_TOKEN_PARAMETER + "=" + URLEncoder.encode(REFRESH_TOKEN, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_REFRESH_TOKEN, StandardCharsets.UTF_8.name()))));
    }
}

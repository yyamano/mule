/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.functional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.request.HttpRequesterBuilder;
import org.mule.module.oauth2.asserter.AuthorizationRequestAsserter;
import org.mule.module.oauth2.asserter.OAuthStateFunctionAsserter;
import org.mule.security.oauth.OAuthConstants;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.transport.NullPayload;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.client.fluent.Request;
import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeMinimalConfigTestCase extends AbstractAuthorizationCodeFunctionalTestCase
{

    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("%s://localhost:%d/authorization", getProtocol(), localHostPort.getNumber()));

    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("%s://localhost:%d" + AUTHORIZE_PATH, getProtocol(), resolveOauthServerPort()));

    private int resolveOauthServerPort()
    {
        return getProtocol().equals("http") ? oauthServerPort.getNumber() : oauthHttpsServerPort.getNumber();
    }

    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("%s://localhost:%d/redirect", getProtocol(), localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("%s://localhost:%d" + TOKEN_PATH, getProtocol(), resolveOauthServerPort()));

    @Override
    protected String getConfigFile()
    {
        return "authorization-code-minimal-config.xml";
    }

    protected String getProtocol()
    {
        return "http";
    }

    @Override
    protected int getTimeoutSystemProperty()
    {
        return 999999;
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        wireMockRule.stubFor(get(urlMatching(AUTHORIZE_PATH + ".*")).willReturn(aResponse().withStatus(200)));

        final HttpRequestConfig requestConfig = muleContext.getRegistry().get("httpsRequestConfig");

        new HttpRequesterBuilder(muleContext)
                .setAddress(localAuthorizationUrl.getValue())
                .setMethod("GET")
                .setConfig(requestConfig)
                .build().process(getTestEvent(NullPayload.getInstance()));

        final List<LoggedRequest> requests = findAll(getRequestedFor(urlMatching(AUTHORIZE_PATH + ".*")));
        assertThat(requests.size(), is(1));

        AuthorizationRequestAsserter.create((requests.get(0)))
                .assertMethodIsGet()
                .assertClientIdIs(clientId.getValue())
                .assertRedirectUriIs(redirectUrl.getValue())
                .assertResponseTypeIsCode();
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        configureWireMockToExpectTokenPathRequestAndReturnJsonResponse();

        Request.Get(redirectUrl.getValue() + "?" + OAuthConstants.CODE_PARAMETER + "=" + AUTHENTICATION_CODE)
                .connectTimeout(REQUEST_TIMEOUT)
                .socketTimeout(REQUEST_TIMEOUT)
                .execute();

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CODE_PARAMETER + "=" + URLEncoder.encode(AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REDIRECT_URI_PARAMETER + "=" + URLEncoder.encode(redirectUrl.getValue(), StandardCharsets.UTF_8.name()))));

        OAuthStateFunctionAsserter.createFrom(muleContext.getExpressionLanguage(), "minimalConfig")
                .assertAccessTokenIs(ACCESS_TOKEN)
                .assertRefreshTokenIs(REFRESH_TOKEN);
    }

    protected void configureWireMockToExpectTokenPathRequestAndReturnJsonResponse()
    {
        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                                     .willReturn(aResponse()
                                                         .withBody("{" +
                                                                   "\"" + OAuthConstants.ACCESS_TOKEN_PARAMETER + "\":\"" + ACCESS_TOKEN + "\"," +
                                                                   "\"" + OAuthConstants.EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
                                                                   "\"" + OAuthConstants.REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"}")));
    }

}

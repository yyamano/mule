/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.mule.security.oauth.OAuthConstants;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeBasicTestCase extends FunctionalTestCase
{

    public static final String TOKEN_PATH = "/token";
    public static final String AUTHENTICATION_CODE = "9WGJOBZXAvSibONGAxVlLuML0e0RhfX4";
    public static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
    public static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
    public static final int EXPIRES_IN = 3897;
    private final DynamicPort localHostPort = new DynamicPort("port1");
    private final DynamicPort fakeOauthServerPort = new DynamicPort("port2");
    @Rule
    public HttpTestClient httpTestClient = new HttpTestClient().disableRedirects().start();
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(fakeOauthServerPort.getNumber());
    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d/authorize", fakeOauthServerPort.getNumber()));
    @Rule
    public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
    @Rule
    public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/redirect", localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, fakeOauthServerPort.getNumber()));


    @Override
    protected String getConfigFile()
    {
        return "local-authorization-url-redirect.xml";
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        httpTestClient.get(localAuthorizationUrl.getValue())
                .as(AssertOauthLocalAuthorizationUrlResponse.class)
                .assertIsRedirectTpUrl(authorizationUrl.getValue())
                .assertClientIdIs(clientId.getValue())
                .assertResponseType()
                .assertRedirectUriIs(redirectUrl.getValue());
    }

    @Test
    public void hitRedirectUrlAndGetToken() throws Exception
    {
        wireMockRule.stubFor(post(urlEqualTo(TOKEN_PATH))
                     .willReturn(aResponse()
                                         .withBody("{" +
                                                   "\"" + OAuthConstants.ACCESS_TOKEN_PARAMETER + "\":\"" + ACCESS_TOKEN + "\"," +
                                                   "\"" + OAuthConstants.EXPIRES_IN_PARAMETER + "\":" + EXPIRES_IN + "," +
                                                   "\"" + OAuthConstants.REFRESH_TOKEN_PARAMETER + "\":\"" + REFRESH_TOKEN + "\"}")));

        httpTestClient.get(redirectUrl.getValue() + "?" + OAuthConstants.CODE_PARAMETER + "=" + AUTHENTICATION_CODE);

        wireMockRule.verify(postRequestedFor(urlEqualTo(TOKEN_PATH))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_ID_PARAMETER + "=" + URLEncoder.encode(clientId.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CODE_PARAMETER + "=" + URLEncoder.encode(AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.CLIENT_SECRET_PARAMETER + "=" + URLEncoder.encode(clientSecret.getValue(), StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.GRANT_TYPE_PARAMETER + "=" + URLEncoder.encode(OAuthConstants.GRANT_TYPE_AUTHENTICATION_CODE, StandardCharsets.UTF_8.name())))
                                    .withRequestBody(containing(OAuthConstants.REDIRECT_URI_PARAMETER + "=" + URLEncoder.encode(redirectUrl.getValue(), StandardCharsets.UTF_8.name()))));
    }

}

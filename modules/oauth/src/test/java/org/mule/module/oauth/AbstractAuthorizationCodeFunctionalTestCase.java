package org.mule.module.oauth;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.junit.Rule;

public abstract class AbstractAuthorizationCodeFunctionalTestCase extends FunctionalTestCase
{
    public static final String TOKEN_PATH = "/token";
    public static final String AUTHENTICATION_CODE = "9WGJOBZXAvSibONGAxVlLuML0e0RhfX4";
    public static final String ACCESS_TOKEN = "rbBQLgJXBEYo83K4Fqs4gu6vpCobc2ya";
    public static final String REFRESH_TOKEN = "cry825cyCs2O0j7tRXXVS4AXNu7hsO5wbWjcBoFFcJePy5zZwuQEevIp6hsUaywp";
    public static final String EXPIRES_IN = "3897";
    public static final String AUTHORIZE_PATH = "/authorize";
    protected final DynamicPort localHostPort = new DynamicPort("port1");
    protected final DynamicPort oauthServerPort = new DynamicPort("port2");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(oauthServerPort.getNumber());
    @Rule
    public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
    @Rule
    public SystemProperty clientSecret = new SystemProperty("client.secret", "yL692Az1cNhfk1VhTzyx4jOjjMKBrO9T");
    @Rule
    public SystemProperty scopes = new SystemProperty("scopes", "expected scope");
    @Rule
    public SystemProperty state = new SystemProperty("state", "expected state");
    @Rule
    public SystemProperty oauthServerPortNumber = new SystemProperty("oauth.server.port", String.valueOf(oauthServerPort.getNumber()));

}

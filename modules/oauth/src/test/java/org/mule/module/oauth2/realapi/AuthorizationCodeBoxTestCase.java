/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.realapi;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

public class AuthorizationCodeBoxTestCase extends FunctionalTestCase
{

    public static final String TOKEN_PATH = "/token";
    private final DynamicPort localHostPort = new DynamicPort("port1");
    private final DynamicPort fakeOauthServerPort = new DynamicPort("port2");
    @Rule
    public SystemProperty localAuthorizationUrl = new SystemProperty("local.authorization.url", String.format("http://localhost:%d/authorization", localHostPort.getNumber()));
    @Rule
    public SystemProperty authorizationUrl = new SystemProperty("authorization.url", String.format("http://localhost:%d/authorize", fakeOauthServerPort.getNumber()));
    @Rule
    public SystemProperty clientId = new SystemProperty("client.id", "ndli93xdws2qoe6ms1d389vl6bxquv3e");
    @Rule
    public SystemProperty redirectUrl = new SystemProperty("redirect.url", String.format("http://localhost:%d/redirect", localHostPort.getNumber()));
    @Rule
    public SystemProperty tokenUrl = new SystemProperty("token.url", String.format("http://localhost:%d" + TOKEN_PATH, fakeOauthServerPort.getNumber()));


    @Override
    protected String getConfigFile()
    {
        return "authorization-code-box-basic.xml";
    }

    @Test
    public void localAuthorizationUrlRedirectsToOAuthAuthorizationUrl() throws Exception
    {
        while (true)
        {
            Thread.sleep(1000);
        }
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 999999;
    }
}

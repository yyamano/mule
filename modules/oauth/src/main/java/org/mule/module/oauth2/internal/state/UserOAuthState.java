/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.state;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth state for a particular user.
 */
public class UserOAuthState
{

    public static final String DEFAULT_USER_ID = "default";

    private String accessToken;
    private String refreshToken;
    private String state;
    private String expiresIn;
    private Map<String, Object> tokenResponseParameters = new HashMap<String, Object>();

    /**
     * @return access token of the oauth state retrieved by the token request
     */
    public String getAccessToken()
    {
        return accessToken;
    }

    /**
     * @return refresh token of the oauth state retrieved by the token request
     */
    public String getRefreshToken()
    {
        return refreshToken;
    }

    /**
     * @return state of the oauth state send in the authorization request
     */
    public String getState()
    {
        return state;
    }

    public void setAccessToken(String accessToken)
    {
        this.accessToken = accessToken;
    }

    public void setRefreshToken(String refreshToken)
    {
        this.refreshToken = refreshToken;
    }

    public void setExpiresIn(String expiresIn)
    {
        this.expiresIn = expiresIn;
    }

    public String getExpiresIn()
    {
        return expiresIn;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    /**
     * @return custom token request response parameters configured for extraction.
     */
    public Map<String, Object> getTokenResponseParameters()
    {
        return tokenResponseParameters;
    }

    public void setTokenResponseParameters(Map<String, Object> tokenResponseParameters)
    {
        this.tokenResponseParameters = tokenResponseParameters;
    }
}

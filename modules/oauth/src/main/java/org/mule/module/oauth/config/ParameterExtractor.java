/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.oauth.config;

public class ParameterExtractor
{

    private String authenticationCode;
    private String state;
    private String accessToken;
    private String refreshToken;
    private String expiresIn;

    public void setAuthenticationCode(String authenticationCode)
    {
        this.authenticationCode = authenticationCode;
    }

    public void setState(String state)
    {
        this.state = state;
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
}

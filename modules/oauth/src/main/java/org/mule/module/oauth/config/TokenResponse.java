package org.mule.module.oauth.config;

import org.mule.security.oauth.OAuthUtils;

import java.util.Collections;
import java.util.List;

public class TokenResponse
{

    private String accessToken = OAuthUtils.ACCESS_TOKEN_EXPRESSION;
    private String refreshToken = OAuthUtils.REFRESH_TOKEN_EXPRESSION;
    private String expiresIn = OAuthUtils.EXPIRATION_TIME_EXPRESSION;

    private List<ParameterExtractor> parameterExtractors = Collections.emptyList();

    public void setParameterExtractors(List<ParameterExtractor> parameterExtractors)
    {
        this.parameterExtractors = parameterExtractors;
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

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getExpiresIn()
    {
        return expiresIn;
    }

    public List<ParameterExtractor> getParameterExtractors()
    {
        return parameterExtractors;
    }
}

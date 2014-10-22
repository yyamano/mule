package org.mule.module.oauth.state;

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

    public String getAccessToken()
    {
        return accessToken;
    }

    public String getRefreshToken()
    {
        return refreshToken;
    }

    public String getState()
    {
        return state;
    }

    public String getAuthenticationParameter(String paramName)
    {
        //TODO
        return null;
    }

    public String getTokenParameter(String paramName)
    {
        //TODO
        return null;
    }

    public void updateAccessToken(String newAccessToken)
    {
        this.accessToken = newAccessToken;
    }

    public void updateRefreshToken(String newRefreshToken)
    {
        this.refreshToken = newRefreshToken;
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

    public Map<String, Object> getTokenResponseParameters()
    {
        return tokenResponseParameters;
    }

    public void setTokenResponseParameters(Map<String, Object> tokenResponseParameters)
    {
        this.tokenResponseParameters = tokenResponseParameters;
    }
}

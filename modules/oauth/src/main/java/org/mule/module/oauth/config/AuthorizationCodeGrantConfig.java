package org.mule.module.oauth.config;

import org.mule.api.MuleEvent;

public interface AuthorizationCodeGrantConfig
{

    String getConfigName();
    String getClientSecret();
    String getClientId();
    String getRedirectionUrl();
    String getRefreshTokenWhen();
    String getOAuthStateId();
    void refreshToken(MuleEvent currentFlowEvent, String userId);
}

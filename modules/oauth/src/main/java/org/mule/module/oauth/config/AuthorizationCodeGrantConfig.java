package org.mule.module.oauth.config;

import org.mule.api.MuleEvent;
import org.mule.module.http.HttpRequestConfig;
import org.mule.module.http.listener.HttpListenerConfig;

public interface AuthorizationCodeGrantConfig
{

    String getConfigName();
    String getClientSecret();
    String getClientId();
    String getRedirectionUrl();
    String getRefreshTokenWhen();
    String getOAuthStateId();
    HttpRequestConfig getRequestConfig();
    HttpListenerConfig getListenerConfig();
    void refreshToken(MuleEvent currentFlowEvent, String userId);
}

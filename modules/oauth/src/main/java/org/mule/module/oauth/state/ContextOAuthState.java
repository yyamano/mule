package org.mule.module.oauth.state;

import java.util.HashMap;
import java.util.Map;

/**
 *  Contains all the OAuth states in a certain mule context.
 */
public class ContextOAuthState
{

    private Map<String, ConfigOAuthState> oauthStatePerConfig = new HashMap<String, ConfigOAuthState>();


    public ConfigOAuthState getStateForConfig(String oauthConfigName)
    {
        if (!oauthStatePerConfig.containsKey(oauthConfigName))
        {
            oauthStatePerConfig.put(oauthConfigName, new ConfigOAuthState());
        }
        return oauthStatePerConfig.get(oauthConfigName);
    }
}

package org.mule.module.oauth.state;

import java.util.HashMap;
import java.util.Map;

/**
 *  Provides the OAuth state for a particular config
 */
public class ConfigOAuthState
{

    private Map<String, UserOAuthState> oauthStatePerUser = new HashMap<String, UserOAuthState>();

    public UserOAuthState getStateForUser(String userId)
    {
        if (!oauthStatePerUser.containsKey(userId))
        {
            oauthStatePerUser.put(userId, new UserOAuthState());
        }
        return oauthStatePerUser.get(userId);
    }
}

package org.mule.module.oauth.mel;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageFunction;
import org.mule.api.registry.RegistrationException;
import org.mule.module.oauth.state.ContextOAuthState;

/**
 * Function oauthState for accessing OAuth authentication state
 */
public class OAuthStateExpressionLanguageFunction implements ExpressionLanguageFunction
{

    private MuleContext muleContext;

    public OAuthStateExpressionLanguageFunction(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public Object call(Object[] params, ExpressionLanguageContext context)
    {
        int numParams = params.length;
        if (numParams < 1 || numParams > 2)
        {
            throw new IllegalArgumentException("invalid number of arguments");
        }

        String oauthConfigName = (String) params[0];
        String userId = "default";
        if (params.length == 2)
        {
            userId = (String) params[1];
        }

        try
        {
            ContextOAuthState contextOAuthState = muleContext.getRegistry().lookupObject(ContextOAuthState.class);
            return contextOAuthState.getStateForConfig(oauthConfigName).getStateForUser(userId);
        }
        catch (RegistrationException e)
        {
            throw new MuleRuntimeException(e);
        }
    }

}

package org.mule.module.oauth.mel;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

/**
 * MEL extension for adding OAuth related functions.
 */
public class OAuthExpressionLanguageExtension implements ExpressionLanguageExtension, MuleContextAware, Initialisable
{

    private MuleContext muleContext;
    private OAuthStateExpressionLanguageFunction oauthStateFunction;

    @Override
    public void initialise() throws InitialisationException
    {
        oauthStateFunction = new OAuthStateExpressionLanguageFunction(muleContext);
    }

    @Override
    public void configureContext(ExpressionLanguageContext context)
    {
        context.declareFunction("oauthState", oauthStateFunction);
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}

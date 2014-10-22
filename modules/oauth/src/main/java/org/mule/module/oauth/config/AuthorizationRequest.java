package org.mule.module.oauth.config;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.http.HttpHeaders;
import org.mule.module.http.listener.HttpListener;
import org.mule.module.http.listener.HttpListenerBuilder;
import org.mule.module.http.listener.HttpResponseBuilder;
import org.mule.module.oauth.AuthorizationRequestUrlBuilder;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationRequest implements MuleContextAware
{

    private Logger logger = LoggerFactory.getLogger(AuthorizationRequest.class);
    private String scopes;
    private String state;
    private String localAuthorizationUrl;
    private String authorizationUrl;
    private Map<String, String> customParameters = new HashMap<String, String>();
    private HttpListener listener;
    private MuleContext muleContext;
    private AuthorizationCodeGrantTypeConfig oauthConfig;

    public void setScopes(String scopes)
    {
        this.scopes = scopes;
    }

    public void setState(String state)
    {
        this.state = state;
    }

    public void setLocalAuthorizationUrl(String localAuthorizationUrl)
    {
        this.localAuthorizationUrl = localAuthorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
    }

    public Map<String, String> getCustomParameters()
    {
        return customParameters;
    }

    public void setCustomParameters(Map<String, String> customParameters)
    {
        this.customParameters = customParameters;
    }

    public void startListener() throws MuleException
    {
        //TODO validate authorization url
        try
        {
            final HttpListenerBuilder httpListenerBuilder = new HttpListenerBuilder(muleContext);
            //TODO verify state is not being used.
            final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder()
                    .setAuthorizationUrl(authorizationUrl)
                    .setClientId(oauthConfig.getClientId())
                    .setClientSecret(oauthConfig.getClientSecret())
                    .setCustomParameters(customParameters)
                    .setRedirectUrl(oauthConfig.getRedirectionUrl())
                    .setState(state)
                    .setScope(scopes).buildUrl();
            final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
            responseBuilder.setStatusCode(302);
            responseBuilder.addHeader(HttpHeaders.Names.LOCATION, authorizationUrlWithParams);
            this.listener = httpListenerBuilder.setUrl(localAuthorizationUrl)
                    .setMuleContext(muleContext)
                    .setResponseBuilder(responseBuilder)
                    .setListener(new MessageProcessor()
            {
                @Override
                public MuleEvent process(MuleEvent muleEvent) throws MuleException
                {
                    return muleEvent;
                }
            }).build();
            this.listener.start();
        }
        catch (MalformedURLException e)
        {
            logger.warn("Could not parse provided url %s. Validate that the url is correct", localAuthorizationUrl);
            throw new DefaultMuleException(e);
        }
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setOauthConfig(AuthorizationCodeGrantTypeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    public AuthorizationCodeGrantTypeConfig getOauthConfig()
    {
        return oauthConfig;
    }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2;

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
import org.mule.util.AttributeEvaluator;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationRequestHandler implements MuleContextAware
{

    private Logger logger = LoggerFactory.getLogger(AuthorizationRequestHandler.class);
    private String scopes;
    private String state;
    private String localAuthorizationUrl;
    private String authorizationUrl;
    private Map<String, String> customParameters = new HashMap<String, String>();
    private HttpListener listener;
    private MuleContext muleContext;
    private AuthorizationCodeConfig oauthConfig;
    private AttributeEvaluator oauthStateIdEvaluator;
    private AttributeEvaluator stateEvaluator;

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
            oauthStateIdEvaluator = new AttributeEvaluator(oauthConfig.getOAuthStateId()).initialize(muleContext.getExpressionManager());
            stateEvaluator = new AttributeEvaluator(state).initialize(muleContext.getExpressionManager());
            final HttpListenerBuilder httpListenerBuilder = new HttpListenerBuilder(muleContext);
            final HttpResponseBuilder responseBuilder = new HttpResponseBuilder();
            responseBuilder.setStatusCode("302");
            responseBuilder.setMuleContext(muleContext);
            responseBuilder.initialise();
            this.listener = httpListenerBuilder.setUrl(localAuthorizationUrl)
                    .setMuleContext(muleContext)
                    .setResponseBuilder(responseBuilder)
                    .setListenerConfig(oauthConfig.getListenerConfig())
                    .setListener(new MessageProcessor()
                    {
                        @Override
                        public MuleEvent process(MuleEvent muleEvent) throws MuleException
                        {
                            final String oauthStateId = oauthStateIdEvaluator.resolveStringValue(muleEvent);
                            muleEvent.setFlowVariable("oauthStateId", oauthStateId);
                            String currentState;
                            if (state == null && oauthStateId != null)
                            {
                                currentState = ":oauthStateId=" + oauthStateId;
                            }
                            else if (oauthStateId != null)
                            {
                                final String stateValue = stateEvaluator.resolveStringValue(muleEvent);
                                currentState = (stateValue == null ? "" : stateValue) + ":oauthStateId=" + oauthStateId;
                            }
                            else
                            {
                                currentState = stateEvaluator.resolveStringValue(muleEvent);
                            }
                            //TODO verify state is not being used.
                            final String authorizationUrlWithParams = new AuthorizationRequestUrlBuilder()
                                    .setAuthorizationUrl(authorizationUrl)
                                    .setClientId(oauthConfig.getClientId())
                                    .setClientSecret(oauthConfig.getClientSecret())
                                    .setCustomParameters(customParameters)
                                    .setRedirectUrl(oauthConfig.getRedirectionUrl())
                                    .setState(currentState)
                                    .setScope(scopes).buildUrl();

                            muleEvent.getMessage().setOutboundProperty(HttpHeaders.Names.LOCATION, authorizationUrlWithParams);
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

    public void setOauthConfig(AuthorizationCodeConfig oauthConfig)
    {
        this.oauthConfig = oauthConfig;
    }

    public AuthorizationCodeConfig getOauthConfig()
    {
        return oauthConfig;
    }
}

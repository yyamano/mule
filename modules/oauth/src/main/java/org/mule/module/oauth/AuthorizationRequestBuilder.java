/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth;

import java.util.HashMap;
import java.util.Map;

public class AuthorizationRequestBuilder
{

    private String authorizationUrl;
    private String redirectUrl;
    private String clientId;
    private String scope;
    private String clientSecret;
    private Map<String, String> customParameters = new HashMap<String, String>();

    public AuthorizationRequestBuilder setAuthorizationUrl(String authorizationUrl)
    {
        this.authorizationUrl = authorizationUrl;
        return this;
    }

    public AuthorizationRequestBuilder setRedirectUrl(String redirectUrl)
    {
        this.redirectUrl = redirectUrl;
        return this;
    }

    public AuthorizationRequestBuilder setClientId(String clientId)
    {
        this.clientId = clientId;
        return this;
    }

    public AuthorizationRequestBuilder setClientSecret(String clientSecret)
    {
        this.clientSecret = clientSecret;
        return this;
    }

    public AuthorizationRequestBuilder setScope(String scope)
    {
        this.scope = scope;
        return this;
    }

    public AuthorizationRequestBuilder setCustomParameters(Map<String, String> customParameters)
    {
        this.customParameters = customParameters;
        return this;
    }

    public AuthorizationRequest build()
    {
        if (redirectUrl != null)
        {
            return new AuthorizationRequest(clientId, clientSecret, scope, authorizationUrl, customParameters);
        }
        else
        {
            return new AuthorizationRequest(clientId, clientSecret, scope, authorizationUrl, customParameters, redirectUrl);
        }
    }

}

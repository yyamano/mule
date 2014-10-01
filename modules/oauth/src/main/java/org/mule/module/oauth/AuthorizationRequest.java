/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.mule.util.Preconditions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthorizationRequest
{

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final String authorizationUrl;
    private final Map<String, String> customParameters;
    private String redirectUrl;

    public AuthorizationRequest(String clientId, String clientSecret, String scope, String authorizationUrl, Map<String, String> customParameters)
    {
        Preconditions.checkArgument(isNotBlank(clientId), "client cannot be blank");
        Preconditions.checkArgument(isNotBlank(clientSecret), "client cannot be blank");
        Preconditions.checkArgument(isNotBlank(authorizationUrl), "client cannot be blank");
        Preconditions.checkArgument(customParameters != null, "client cannot be null");
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.scope = scope;
        this.authorizationUrl = authorizationUrl;
        this.customParameters = customParameters;
        this.redirectUrl = null;
        buildAuthorizeUrl();
    }

    public AuthorizationRequest(String clientId, String clientSecret, String scope, String authorizationUrl, Map<String, String> customParameters, String redirectUrl)
    {
        this(clientId, clientSecret, scope, authorizationUrl, customParameters);
        Preconditions.checkArgument(redirectUrl != null && !redirectUrl.isEmpty(), "redirectUrl cannot be null or empty");
        this.redirectUrl = redirectUrl;
    }

    public final String buildAuthorizeUrl()
    {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(authorizationUrl);

        urlBuilder.append("?")
                .append("response_type=code&")
                .append("client_id=")
                .append(clientId);

        try
        {
            if (isNotBlank(scope))
            {
                urlBuilder.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
            }

            for (Map.Entry<String, String> entry : customParameters.entrySet())
            {
                urlBuilder.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUrl, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(("Authorization URL has been generated as follows: " + urlBuilder));
        }
        return urlBuilder.toString();
    }
}

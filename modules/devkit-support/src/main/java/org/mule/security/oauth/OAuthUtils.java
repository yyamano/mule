/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.security.oauth.exception.AuthorizationCodeNotFoundException;
import org.mule.security.oauth.util.HttpUtil;
import org.mule.security.oauth.util.HttpUtilImpl;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class OAuthUtils
{

    public final static Pattern ACCESS_CODE_PATTERN = Pattern.compile("\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\"");
    public final static Pattern REFRESH_TOKEN_PATTERN = Pattern.compile("\"refresh_token\"[ ]*:[ ]*\"([^\\\"]*)\"");
    public final static Pattern EXPIRATION_TIME_PATTERN = Pattern.compile("\"expires_in\"[ ]*:[ ]*([\\d]*)");

    private static HttpUtil httpUtil = new HttpUtilImpl();

    public static String buildAuthorizeUrl(String clientId, String authorizationUrl, String redirectUri, String scope, String state, Map<String, String> extraParameters)
    {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(authorizationUrl);

        urlBuilder.append("?")
                .append(OAuthConstants.RESPONSE_TYPE_PARAMETER + "=code&")
                .append(OAuthConstants.CLIENT_ID_PARAMETER + "=")
                .append(clientId);

        try
        {
            if (!StringUtils.isBlank(scope))
            {
                urlBuilder.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
            }

            if (!StringUtils.isBlank(state))
            {
                urlBuilder.append("&state=").append(URLEncoder.encode(state, "UTF-8"));
            }

            for (Map.Entry<String, String> entry : extraParameters.entrySet())
            {
                urlBuilder.append("&")
                        .append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }

            urlBuilder.append("&redirect_uri=").append(URLEncoder.encode(redirectUri, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }

        return urlBuilder.toString();
    }

    public static String buildTokenBody(String clientId, String clientSecret, String redirectUrl, String code)
    {
        StringBuilder builder = new StringBuilder();
        try
        {
            builder.append("code=");
            builder.append(URLEncoder.encode(code, "UTF-8"));
            builder.append("&client_id=");
            builder.append(URLEncoder.encode(clientId, "UTF-8"));
            builder.append("&client_secret=");
            builder.append(URLEncoder.encode(clientSecret, "UTF-8"));
            builder.append("&grant_type=");
            builder.append(URLEncoder.encode("authorization_code", "UTF-8"));
            builder.append("&redirect_uri=");
            builder.append(URLEncoder.encode(redirectUrl, "UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException(e);
        }
        return builder.toString();
    }

    public static String callTokenUrl(String tokenUrl, String requestBody)
    {
        return httpUtil.post(tokenUrl, requestBody);
    }

    /**
     * Extract the authentication code from the response body a authentication
     * request call.
     *
     * @param requestBody body of the http response form the oauth authentication url call.
     * @param pattern pattern to extract the authentication code from the response
     * @return the authetication code
     * @throws Exception TODO document and change exception type to something better or make it runtime if there's nothing we can do.
     */
    public static String extractAuthorizationCode(String requestBody, Pattern pattern) throws Exception
    {
        Matcher matcher = pattern.matcher(requestBody);
        if (matcher.find() && (matcher.groupCount() >= 1))
        {
            return URLDecoder.decode(matcher.group(1), "UTF-8");
        }
        else
        {
            throw new AuthorizationCodeNotFoundException(pattern, requestBody);
        }
    }

    /**
     * Extract the authentication code from the response body a authentication
     * request call using a default pattern against the response to extract the code.
     *
     * @param requestBody body of the http response form the oauth authentication url call.
     * @return the authetication code
     * @throws Exception TODO document and change exception type to something better or make it runtime if there's nothing we can do.
     */
    public static String extractAuthorizationCode(String requestBody) throws Exception
    {
        //TODO fix this. Request parameters are expected.
        return splitQuery(new URL(requestBody)).get("code");
        //return extractAuthorizationCode(requestBody, defaultAuthorizationCodePattern);
    }

    public static String extractState(String requestBody) throws Exception
    {
        //TODO fix this. Request parameters are expected.
        return splitQuery(new URL(requestBody)).get("state");
    }

    public static String extractAccessToken(String tokenUrlCallResponse) throws Exception
    {
        return extractContentUsingPattern(tokenUrlCallResponse, ACCESS_CODE_PATTERN);
    }

    public static String extractRefreshToken(String tokenUrlCallResponse) throws Exception
    {
        return extractContentUsingPattern(tokenUrlCallResponse, REFRESH_TOKEN_PATTERN);
    }

    private static String extractContentUsingPattern(String tokenUrlCallResponse, Pattern pattern) throws UnsupportedEncodingException, AuthorizationCodeNotFoundException
    {
        Matcher matcher = pattern.matcher(tokenUrlCallResponse);
        if (matcher.find() && (matcher.groupCount() >= 1))
        {
            return URLDecoder.decode(matcher.group(1), "UTF-8");
        }
        else
        {
            throw new AuthorizationCodeNotFoundException(pattern, tokenUrlCallResponse);
        }
    }


    public static String extractExpirationTime(String tokenUrlCallResponse) throws UnsupportedEncodingException, AuthorizationCodeNotFoundException
    {
        return extractContentUsingPattern(tokenUrlCallResponse, EXPIRATION_TIME_PATTERN);
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException
    {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }
}

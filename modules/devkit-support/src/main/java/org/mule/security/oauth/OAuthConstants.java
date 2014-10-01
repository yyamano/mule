package org.mule.security.oauth;

//TODO apply constants to all the code.
public interface OAuthConstants
{

    //Parameters
    String GRANT_TYPE_PARAMETER = "grant_type";
    String REDIRECT_URI_PARAMETER = "redirect_uri";
    String CLIENT_SECRET_PARAMETER = "client_secret";
    String CODE_PARAMETER = "code";
    String CLIENT_ID_PARAMETER = "client_id";
    String ACCESS_TOKEN_PARAMETER = "access_token";
    String EXPIRES_IN_PARAMETER = "expires_in";
    String REFRESH_TOKEN_PARAMETER = "refresh_token";
    String RESPONSE_TYPE_PARAMETER = "response_type";

    //Values
    String GRANT_TYPE_AUTHENTICATION_CODE = "authorization_code";
}

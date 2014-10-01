package org.mule.module.oauth;

import org.mule.security.oauth.OAuthConstants;

import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;

public class AssertOauthLocalAuthorizationUrlResponse extends AssertHttpResponse
{

    public static final int REDIRECT = 302;

    public AssertOauthLocalAuthorizationUrlResponse(CloseableHttpResponse response)
    {
        super(response);
    }


    public AssertOauthLocalAuthorizationUrlResponse assertIsRedirectTpUrl(String url)
    {
        assertStatusCodeIs(REDIRECT);
        assertHasHeader(HttpHeaders.LOCATION);
        getLocationAssertUrl().urlWithoutQueryIs(url);
        return this;
    }

    public AssertOauthLocalAuthorizationUrlResponse assertResponseType()
    {
        getLocationAssertUrl().hasQueryParamWithValue(OAuthConstants.RESPONSE_TYPE_PARAMETER, "code");
        return this;
    }

    public AssertOauthLocalAuthorizationUrlResponse assertRedirectUriIs(String redirectUri)
    {
        getLocationAssertUrl().hasQueryParamWithValue(OAuthConstants.REDIRECT_URI_PARAMETER, redirectUri);
        return this;
    }

    public AssertOauthLocalAuthorizationUrlResponse assertClientIdIs(String clientIdValue)
    {
        final AssertUrl locationAssertUrl = getLocationAssertUrl()
                .hasQueryParamWithValue("clientId", clientIdValue);
        return this;
    }

    private AssertUrl getLocationAssertUrl()
    {
        return new AssertUrl(response.getFirstHeader("Location").getValue());
    }
}

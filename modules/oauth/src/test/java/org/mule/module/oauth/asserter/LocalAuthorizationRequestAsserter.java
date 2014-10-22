package org.mule.module.oauth.asserter;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.module.http.HttpParser;
import org.mule.module.http.ParameterMap;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

public class LocalAuthorizationRequestAsserter
{

    private final LoggedRequest loggedRequest;
    private final ParameterMap queryParameters;

    public static LocalAuthorizationRequestAsserter create(LoggedRequest loggedRequest)
    {
        return new LocalAuthorizationRequestAsserter(loggedRequest);
    }

    private LocalAuthorizationRequestAsserter(LoggedRequest loggedRequest)
    {
        this.loggedRequest = loggedRequest;
        queryParameters = HttpParser.decodeQueryString(HttpParser.extractQueryParams(loggedRequest.getUrl()));
    }

    public LocalAuthorizationRequestAsserter assertMethodIsGet()
    {
        assertThat(loggedRequest.getMethod().value(), is("GET"));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertClientIdIs(String expectedClientId)
    {
        assertThat(queryParameters.get("client_id"), is(expectedClientId));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertRedirectUriIs(String redirectUri)
    {
        assertThat(queryParameters.get("redirect_uri"), is(redirectUri));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertResponseTypeIsCode()
    {
        assertThat(queryParameters.get("response_type"), is("code"));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertScopeIs(String expectedScope)
    {
        assertThat(queryParameters.get("scope"), is(expectedScope));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertStateIs(String expectedState)
    {
        assertThat(queryParameters.get("state"), is(expectedState));
        return this;
    }

    public LocalAuthorizationRequestAsserter assertContainsCustomParameter(String paramName, String paramValue)
    {
        assertThat(queryParameters.get(paramName), is(paramValue));
        return this;
    }
}

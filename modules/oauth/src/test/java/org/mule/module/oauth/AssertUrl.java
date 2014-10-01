package org.mule.module.oauth;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.client.utils.URLEncodedUtils;

public class AssertUrl
{

    private final URL url;

    public AssertUrl(String urlAsString)
    {
        try
        {
            this.url = new URL(urlAsString);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public AssertUrl urlWithoutQueryIs(String urlAsString)
    {
        assertThat(url.toString().replace("?" + url.getQuery(), ""), is(urlAsString));
        return this;
    }

    public AssertUrl hasQueryParamWithValue(String clientId, String clientIdValue)
    {
        URLEncodedUtils.parse(url.getQuery(), Charset.defaultCharset());
        return this;
    }
}

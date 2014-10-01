package org.mule.module.oauth;

import java.io.IOException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.junit.rules.ExternalResource;

public class HttpTestClient extends ExternalResource
{

    private boolean initialized;
    private boolean disableRedirects;
    private CloseableHttpClient httpClient;

    public HttpTestClient disableRedirects()
    {
        this.disableRedirects = true;
        return this;
    }

    public HttpTestClient start()
    {
        final HttpClientBuilder httpClientBuilder = HttpClients.custom();
        if (this.disableRedirects)
        {
            httpClientBuilder.setRedirectStrategy(new RedirectStrategy()
            {
                @Override
                public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException
                {
                    return false;
                }

                @Override
                public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws ProtocolException
                {
                    return null;
                }
            });
        }
        this.httpClient = httpClientBuilder.build();
        return this;
    }



    @Override
    protected void before() throws Throwable
    {
        if (initialized)
        {
            throw new IllegalArgumentException("HttpTestClient was already initialized");
        }
        initialized = true;
    }

    @Override
    protected void after()
    {
        if (!initialized)
        {
            throw new IllegalArgumentException("HttpTestClient was not initialized");
        }

        try
        {
            httpClient.close();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        initialized = false;
    }

    public AssertHttpResponse get(String url)
    {
        try
        {
            HttpGet httpGet = new HttpGet(url);
            final CloseableHttpResponse response = httpClient.execute(httpGet);
            return new AssertHttpResponse(response);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}


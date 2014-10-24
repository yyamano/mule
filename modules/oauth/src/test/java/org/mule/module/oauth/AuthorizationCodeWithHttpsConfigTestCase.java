package org.mule.module.oauth;

public class AuthorizationCodeWithHttpsConfigTestCase extends AuthorizationCodeMinimalConfigTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "authorization-code-https-config.xml";
    }

    @Override
    protected String getProtocol()
    {
        return "https";
    }

}

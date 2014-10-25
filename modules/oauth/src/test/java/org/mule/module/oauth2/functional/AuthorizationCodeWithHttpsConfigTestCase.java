/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.functional;

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

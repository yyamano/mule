/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.context.ApplicationDomainContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.MuleApplicationDomain;

import java.net.URL;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class MuleApplicationDomainContextBuilder implements ApplicationDomainContextBuilder
{

    private String domain;
    private ClassLoader classLoader;
    private String domainConfigFileLocation = "mule-domain-config.xml";

    @Override
    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    @Override
    public void setClassLoader(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    @Override
    public MuleApplicationDomain build()
    {
        try
        {
            //TODO add logging
            URL resource = classLoader.getResource(this.domainConfigFileLocation);
            ApplicationContext domainApplicationContext = null;
            MuleContext muleContext = null;
            if (resource != null)
            {
                muleContext = new DefaultMuleContextFactory().createMuleContext();
                SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(new String[] {domainConfigFileLocation});
                springXmlConfigurationBuilder.setUseMinimalConfigResource(true);
                springXmlConfigurationBuilder.doConfigure(muleContext);
                domainApplicationContext = springXmlConfigurationBuilder.getApplicationContext();
            }
            MuleApplicationDomain muleApplicationDomain = new MuleApplicationDomain(domain, muleContext, domainApplicationContext);
            return muleApplicationDomain;
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    public void setDomainConfigFileLocation(String domainConfigFileLocation)
    {
        this.domainConfigFileLocation = domainConfigFileLocation;
    }
}

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
            URL resource = classLoader.getResource("mule-domain-config.xml");
            ApplicationContext domainApplicationContext = null;
            if (resource != null)
            {
                MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext();
                SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(new String[] {"mule-domain-config.xml"});
                springXmlConfigurationBuilder.setUseMinimalConfigResource(true);
                springXmlConfigurationBuilder.doConfigure(muleContext);
                domainApplicationContext = springXmlConfigurationBuilder.getApplicationContext();
                muleContext.start();
            }
            return new MuleApplicationDomain(domain,domainApplicationContext);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }
}

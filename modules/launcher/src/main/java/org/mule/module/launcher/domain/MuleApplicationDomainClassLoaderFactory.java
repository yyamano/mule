package org.mule.module.launcher.domain;

import org.mule.module.launcher.DefaultMuleSharedDomainClassLoader;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.util.StringUtils;

public class MuleApplicationDomainClassLoaderFactory implements ApplicationDomainClassLoaderFactory
{

    @Override
    public ClassLoader create(String domain)
    {
        ClassLoader classLoader;
        if (StringUtils.isBlank(domain) || DefaultMuleSharedDomainClassLoader.DEFAULT_DOMAIN_NAME.equals(domain))
        {
            classLoader = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        }
        else
        {
            classLoader = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader());
        }
        return classLoader;
    }
}

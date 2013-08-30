package org.mule.module.launcher.domain;

public interface ApplicationDomainClassLoaderFactory
{

    ClassLoader create(String domain);

}

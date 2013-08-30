package org.mule.context;

/**
 *
 */
public interface ApplicationDomainContextBuilder
{

    void setDomain(String domain);
    void setClassLoader(ClassLoader classLoader);
    MuleApplicationDomain build();
}

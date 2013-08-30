package org.mule.module.launcher.domain;

import org.mule.context.MuleApplicationDomain;

import java.io.IOException;

/**
 *
 */
public interface ApplicationDomainFactory
{

    public MuleApplicationDomain createAppDomain(String appName) throws IOException;

    void createAllDomains();
}

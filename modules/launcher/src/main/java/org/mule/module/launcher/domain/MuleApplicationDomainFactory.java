/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.context.DefaultMuleDomainFactory;
import org.mule.context.MuleApplicationDomain;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MuleApplicationDomainFactory implements ApplicationDomainFactory
{
    private Map<String, MuleApplicationDomain> domains = new HashMap<String, MuleApplicationDomain>();

    @Override
    public MuleApplicationDomain createAppDomain(String domainName) throws IOException
    {
        //TODO validate null pointer
        return domains.get(domainName);
    }

    @Override
    public void createAllDomains()
    {
        File domainFolder = MuleContainerBootstrapUtils.getMuleDomainsDir();
        Map<String, ClassLoader> domainClassLoader = new HashMap<String, ClassLoader>();
        FileFilter onlyDirectoriesFilter = new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return file.isDirectory();
            }
        };
        File[] domainFolders = domainFolder.listFiles(onlyDirectoriesFilter);
        if (domainFolder != null && domainFolders.length > 0)
        {
            for (File domainDir : domainFolders)
            {
                domainClassLoader.put(domainDir.getName(),new MuleSharedDomainClassLoader(domainDir.getName(),getClass().getClassLoader()));
            }

            for (String domain : domainClassLoader.keySet())
            {
                DefaultMuleDomainFactory defaultMuleDomainFactory = new DefaultMuleDomainFactory();
                MuleApplicationDomain muleDomain = defaultMuleDomainFactory.createMuleDomain(domain, domainClassLoader.get(domain));
                domains.put(domain,muleDomain);
            }
        }
    }
}

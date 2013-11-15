/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.api.MuleRuntimeException;
import org.mule.context.ApplicationDomainContextBuilder;
import org.mule.context.MuleApplicationDomain;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class DefaultDomainFactory implements DomainFactory
{
    public static final String DOMAIN_CONTEXT_BUILDER = "org.mule.config.spring.MuleApplicationDomainContextBuilder";

    private Map<String, Domain> domains = new HashMap<String, Domain>();

    @Override
    public Domain createAppDomain(String domainName) throws IOException
    {
        //TODO validate null pointer
        return domains.get(domainName);
    }
    //
    //@Override
    //public List<Domain> createAllDomains()
    //{
    //    File domainFolder = MuleContainerBootstrapUtils.getMuleDomainsDir();
    //    Map<String, ClassLoader> domainClassLoader = new HashMap<String, ClassLoader>();
    //    FileFilter onlyDirectoriesFilter = new FileFilter()
    //    {
    //        @Override
    //        public boolean accept(File file)
    //        {
    //            return file.isDirectory();
    //        }
    //    };
    //    File[] domainFolders = domainFolder.listFiles(onlyDirectoriesFilter);
    //    if (domainFolder != null && domainFolders.length > 0)
    //    {
    //        for (File domainDir : domainFolders)
    //        {
    //            domainClassLoader.put(domainDir.getName(),new MuleSharedDomainClassLoader(domainDir.getName(),getClass().getClassLoader()));
    //        }
    //
    //        for (String domain : domainClassLoader.keySet())
    //        {
    //            DefaultMuleDomainFactory defaultMuleDomainFactory = new DefaultMuleDomainFactory();
    //            MuleApplicationDomain muleDomain = defaultMuleDomainFactory.createMuleDomain(domain, domainClassLoader.get(domain));
    //            domains.put(domain, (Domain) muleDomain);
    //        }
    //    }
    //    return (List<Domain>) Collections.unmodifiableCollection(domains.values());
    //}

    @Override
    public Domain createArtifact(String artifactName) throws IOException
    {
        MuleSharedDomainClassLoader muleSharedDomainClassLoader = new MuleSharedDomainClassLoader(artifactName, getClass().getClassLoader());
        //TODO review how to get this instance without depending on spring module
        try
        {
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(muleSharedDomainClassLoader);
            try
            {
                ApplicationDomainContextBuilder applicationDomainContextBuilder =  (ApplicationDomainContextBuilder) Class.forName(DOMAIN_CONTEXT_BUILDER).newInstance();
                applicationDomainContextBuilder.setDomain(artifactName);
                applicationDomainContextBuilder.setClassLoader(muleSharedDomainClassLoader);
                applicationDomainContextBuilder.setClassLoader(muleSharedDomainClassLoader);
                MuleApplicationDomain muleApplicationDomain = applicationDomainContextBuilder.build();
                return new DefaultMuleDomain(artifactName, muleApplicationDomain.getMuleContext(), muleApplicationDomain.getContext());
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public File getArtifactDir()
    {
        return MuleContainerBootstrapUtils.getMuleDomainsDir();
    }
}

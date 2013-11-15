/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Load $MULE_HOME/lib/shared/<domain> libraries.
 */
public class MuleSharedDomainClassLoader extends GoodCitizenClassLoader
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private String domain = "undefined";

    @SuppressWarnings("unchecked")
    public MuleSharedDomainClassLoader(String domain, ClassLoader parent)
    {
        super(new URL[0], parent);
        try
        {
            this.domain = domain;

            //TODO add support for old domain dir.
            //File oldDomainDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "lib/shared/" + domain);
            //if (!oldDomainDir.exists())
            //{
            //    throw new IllegalArgumentException(
            //            String.format("Shared ClassLoader Domain '%s' doesn't exist", domain));
            //}
            //
            //if (!oldDomainDir.canRead())
            //{
            //    throw new IllegalArgumentException(
            //            String.format("Shared ClassLoader Domain '%s' is not accessible", domain));
            //}

            File newDomainDir = new File(MuleContainerBootstrapUtils.getMuleDomainsDir() + File.separator + domain + File.separator + "lib");
            if (!newDomainDir.exists())
            {
                throw new IllegalArgumentException(
                        String.format("Shared ClassLoader Domain '%s' doesn't exist", domain));
            }

            if (!newDomainDir.canRead())
            {
                throw new IllegalArgumentException(
                        String.format("Shared ClassLoader Domain '%s' is not accessible", domain));
            }

            Collection<File> jars = FileUtils.listFiles(newDomainDir, new String[] {"jar"}, false);

            if (logger.isDebugEnabled())
            {
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Loading Shared ClassLoader Domain: ").append(domain).append(SystemUtils.LINE_SEPARATOR);
                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    for (File jar : jars)
                    {
                        sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                    }

                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    logger.debug(sb.toString());
                }
            }

            for (File jar : jars)
            {
                addURL(jar.toURI().toURL());
            }
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    public String getDomain()
    {
        return domain;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             domain,
                             Integer.toHexString(System.identityHashCode(this)));
    }

    @Override
    public URL findResource(String name)
    {
        URL resource = super.findResource(name);
        if (resource == null)
        {
            File file = new File(MuleContainerBootstrapUtils.getMuleHome(), "lib/shared/" + domain + File.separator + name);
            if (file.exists())
            {
                try
                {
                    resource = file.toURI().toURL();
                }
                catch (MalformedURLException e)
                {
                    logger.debug("Failure looking for resource", e);
                }
            }
        }
        return resource;
    }
}

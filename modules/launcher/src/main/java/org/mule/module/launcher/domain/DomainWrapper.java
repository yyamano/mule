package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;

import java.io.File;

/**
 * Domain wrapper used to notify domain factory that a domain has been disposed.
 */
public class DomainWrapper implements Domain
{

    private final DefaultDomainFactory domainFactory;
    private final Domain delegate;

    protected DomainWrapper(final Domain delegate, final DefaultDomainFactory domainFactory)
    {
        this.delegate = delegate;
        this.domainFactory = domainFactory;
    }

    @Override
    public boolean containsSharedResources()
    {
        return delegate.containsSharedResources();
    }

    @Override
    public MuleContext getMuleContext()
    {
        return delegate.getMuleContext();
    }

    @Override
    public void install() throws InstallException
    {
        delegate.install();
    }

    @Override
    public void init()
    {
        delegate.init();
    }

    @Override
    public void start() throws DeploymentStartException
    {
        delegate.start();
    }

    @Override
    public void stop()
    {
        delegate.stop();
    }

    @Override
    public void dispose()
    {
        try
        {
            delegate.dispose();
        }
        finally
        {
            domainFactory.dispose(delegate);
        }
    }

    @Override
    public void redeploy()
    {
        delegate.redeploy();
    }

    @Override
    public String getArtifactName()
    {
        return delegate.getArtifactName();
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        return delegate.getConfigResourcesFile();
    }
}

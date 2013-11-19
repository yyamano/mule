package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;

import java.io.File;

/**
 *
 */
public class TestDomainWrapper implements Domain
{
    private Domain delegate;
    private boolean failOnPurpose;
    private boolean failOnDispose;

    public TestDomainWrapper(Domain delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean containsSharedResources()
    {
        return delegate.containsSharedResources();
    }

    @Override
    public Object getContext()
    {
        return delegate.getContext();
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
        if (failOnPurpose)
        {
            fail();
        }
        delegate.stop();
    }

    private void fail()
    {
        throw new RuntimeException("fail on purpose");
    }

    @Override
    public void dispose()
    {
        if (failOnDispose)
        {
            fail();
        }
        delegate.dispose();
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

    public void setFailOnStop()
    {
        this.failOnPurpose = true;
    }

    public void setFailOnDispose()
    {
        this.failOnDispose = true;
    }
}

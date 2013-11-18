package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.InstallException;

import java.io.File;

public class DefaultMuleDomain implements Domain
{

    private final MuleContext muleContext;
    private String domain;
    private Object context;
    private boolean domainSuccessfullyDeployed;

    public DefaultMuleDomain(String domain, MuleContext muleContext, Object context)
    {
        this.domain = domain;
        this.muleContext = muleContext;
        this.context = context;
    }

    public String getName()
    {
        return domain;
    }

    public Object getContext()
    {
        return context;
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public void install() throws InstallException
    {
    }

    @Override
    public void init()
    {
    }

    @Override
    public void start()
    {
        try
        {
            if (this.muleContext != null)
            {
                this.muleContext.start();
            }
            domainSuccessfullyDeployed = true;
        }
        catch (Exception e)
        {
            throw new DeploymentStartException(CoreMessages.createStaticMessage("Failure trying to start domain " + getArtifactName()), e);
        }
    }

    @Override
    public void stop()
    {
        try
        {
            if (this.muleContext != null)
            {
                this.muleContext.stop();
            }
        }
        catch (Exception e)
        {
            throw new DeploymentStopException(CoreMessages.createStaticMessage("Failure trying to stop domain " + getArtifactName()), e);
        }
    }

    @Override
    public void dispose()
    {
        if (this.muleContext != null)
        {
            this.muleContext.dispose();
        }
    }

    @Override
    public void redeploy()
    {
    }

    @Override
    public String getArtifactName()
    {
        return domain;
    }

    @Override
    public File[] getConfigResourcesFile()
    {
        //TODO change to retrieve actual config resources
        return new File[0];
    }

    public void initialise()
    {
        try
        {
            if (this.muleContext != null)
            {
                this.muleContext.initialise();
            }
        }
        catch (InitialisationException e)
        {
            throw new DeploymentInitException(CoreMessages.createStaticMessage("Failure trying to initialise domain " + getArtifactName()), e);
        }
    }

    public boolean isDomainSuccessfullyDeployed()
    {
        return domainSuccessfullyDeployed;
    }

    public boolean containsSharedResources()
    {
        return this.context != null;
    }
}

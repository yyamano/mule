package org.mule.module.launcher.domain;

import java.io.IOException;

/**
 *
 */
public class TestDomainFactory extends DefaultDomainFactory
{

    private boolean failOnStop;
    private boolean failOnDispose;

    public TestDomainFactory(DomainClassLoaderFactory domainClassLoaderFactory)
    {
        super(domainClassLoaderFactory);
    }

    @Override
    public Domain createArtifact(String artifactName) throws IOException
    {
        TestDomainWrapper testDomainWrapper = new TestDomainWrapper(super.createArtifact(artifactName));
        if (this.failOnStop)
        {
            testDomainWrapper.setFailOnStop();
        }
        if (this.failOnDispose)
        {
            testDomainWrapper.setFailOnDispose();
        }
        return testDomainWrapper;
    }

    public void setFailOnStopApplication()
    {
        failOnStop = true;
    }

    public void setFailOnDisposeApplication()
    {
        failOnDispose = true;
    }

}

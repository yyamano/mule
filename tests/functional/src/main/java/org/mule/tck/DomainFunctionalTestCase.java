/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.context.MuleContextBuilder;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.context.MuleApplicationDomain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.ClassUtils;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;

public abstract class DomainFunctionalTestCase extends org.mule.tck.junit4.AbstractMuleTestCase
{

    private List<MuleContext> muleContexts = new ArrayList<MuleContext>();
    private List<MuleContext> disposedContexts = new ArrayList<MuleContext>();
    private MuleApplicationDomain domain;

    protected abstract String getDomainConfig();

    public synchronized void disposeMuleContext(MuleContext muleContext)
    {
        disposedContexts.add(muleContext);
        muleContext.dispose();
        while (!muleContext.isDisposed())
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    protected ConfigurationBuilder getBuilder(String configResource) throws Exception
    {
        SpringXmlConfigurationBuilder springXmlConfigurationBuilder = new SpringXmlConfigurationBuilder(configResource);
        springXmlConfigurationBuilder.setDomainContext(domain.getContext());
        return springXmlConfigurationBuilder;
    }

    @Before
    public void setUpMuleContexts() throws Exception
    {
        createDomain();
        String[] configResources = getConfigResources();

        for (String configResource : configResources)
        {
            MuleContext muleContext = createMuleContext(configResource);
            muleContext.start();
            muleContexts.add(muleContext);
        }
    }

    private void createDomain()
    {
        //DefaultMuleDomainFactory defaultMuleDomainFactory = new DefaultMuleDomainFactory();
        //MuleApplicationDomainContextBuilder applicationDomainContextBuilder = new MuleApplicationDomainContextBuilder();
        //defaultMuleDomainFactory.setApplicationDomainContextBuilder(applicationDomainContextBuilder);
        //applicationDomainContextBuilder.setDomainConfigFileLocation(getDomainConfig());
        //domain = defaultMuleDomainFactory.createMuleDomain("domain", getClass().getClassLoader());
    }

    @After
    public void disposeMuleContexts()
    {
        for (MuleContext muleContext : muleContexts)
        {
            try
            {
                disposeMuleContext(muleContext);
            }
            catch (Exception e)
            {
                //Nothing to do
            }
        }
        muleContexts.clear();
    }

    protected MuleContext createMuleContext(String configResource) throws Exception
    {
        // Should we set up the manager for every method?
        MuleContext context;
        MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
        List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>();
        //If the annotations module is on the classpath, add the annotations config builder to the list
        //This will enable annotations config for this instance
        if (ClassUtils.isClassOnPath(AbstractMuleContextTestCase.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            builders.add((ConfigurationBuilder) ClassUtils.instanciateClass(AbstractMuleContextTestCase.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER,
                                                                            ClassUtils.NO_ARGS, getClass()));
        }
        builders.add(getBuilder(configResource));
        DefaultMuleContextBuilder contextBuilder = new DefaultMuleContextBuilder();
        configureMuleContext(contextBuilder);
        context = muleContextFactory.createMuleContext(builders, contextBuilder);
        //context.getRegistry().unregisterAgent(JmxAgent.NAME);
        if (!isGracefulShutdown())
        {
            ((DefaultMuleConfiguration) context.getConfiguration()).setShutdownTimeout(0);
        }
        return context;
    }

    /**
     * Override this method to set properties of the MuleContextBuilder before it is
     * used to create the MuleContext.
     */
    protected void configureMuleContext(MuleContextBuilder contextBuilder)
    {
        contextBuilder.setWorkListener(new TestingWorkListener());
    }

    protected void waitUntilThereIsPrimaryPollingNode()
    {
        PollingProber pollingProber = new PollingProber(5000, 500);
        pollingProber.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                for (MuleContext muleContext : muleContexts)
                {
                    if (muleContext.isStarted() && muleContext.isPrimaryPollingInstance())
                    {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public String describeFailure()
            {
                return "no node became primary";
            }
        });
    }

    /**
     * Determines if the test case should perform graceful shutdown or not.
     * Default is false so that tests run more quickly.
     */
    protected boolean isGracefulShutdown()
    {
        return false;
    }

    public int getNumberOfNodes()
    {
        return 2;
    }

    public abstract String[] getConfigResources();

    public MuleContext getMuleContext(int i)
    {
        return muleContexts.get(i);
    }

}

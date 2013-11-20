/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.DefaultMuleContext;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.context.DefaultMuleContextBuilder;
import org.mule.context.notification.ServerNotificationManager;
import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.expression.DefaultExpressionManager;
import org.mule.lifecycle.MuleContextLifecycleManager;
import org.mule.module.launcher.application.MuleContextDelegateWrapper;
import org.mule.registry.DefaultRegistryBroker;
import org.mule.registry.MuleRegistryHelper;

import javax.resource.spi.work.WorkListener;

public abstract class ArtifactMuleContextBuilder  extends DefaultMuleContextBuilder
{
    private final MuleContextDelegateWrapper muleContextDelegate = new MuleContextDelegateWrapper();
    private MuleContext muleContext;

    @Override
    public MuleContext buildMuleContext()
    {
        logger.debug("Building new DefaultMuleContext instance with MuleContextBuilder: " + this);
        MuleContextLifecycleManager lifecycleManager = getLifecycleManager();
        DefaultRegistryBroker registryBroker = new DefaultRegistryBroker(muleContextDelegate);
        MuleRegistryHelper registry = new MuleRegistryHelper(registryBroker, muleContextDelegate);
        WorkManager workManager = getWorkManager();
        MuleConfiguration muleConfiguration = getMuleConfiguration();
        WorkListener workListener = getWorkListener();
        ServerNotificationManager notificationManager = getNotificationManager();
        DefaultExpressionManager expressionManager = new DefaultExpressionManager();
        DefaultSystemExceptionStrategy defaultSystemExceptionStrategy = new DefaultSystemExceptionStrategy(muleContextDelegate);
        DefaultLocalMuleClient defaultLocalMuleClient = new DefaultLocalMuleClient(muleContextDelegate);
        muleContext = new DefaultMuleContext(injectMuleContextIfRequired(muleConfiguration),
                                                                injectMuleContextIfRequired(workManager),
                                                                injectMuleContextIfRequired(workListener),
                                                                injectMuleContextIfRequired(lifecycleManager),
                                                                injectMuleContextIfRequired(notificationManager),
                                                                injectMuleContextIfRequired(registryBroker),
                                                                injectMuleContextIfRequired(registry),
                                                                injectMuleContextIfRequired(expressionManager),
                                                                injectMuleContextIfRequired(defaultSystemExceptionStrategy),
                                                                injectMuleContextIfRequired(defaultLocalMuleClient));
        muleContext.setExecutionClassLoader(Thread.currentThread().getContextClassLoader());
        configureClassLoaderMuleContext(muleContext);
        return muleContextDelegate;
    }

    /**
     * Allows to create a relation between the artifact and the real MuleContext
     * @param muleContext
     */
    protected abstract void configureClassLoaderMuleContext(MuleContext muleContext);

    public <T> T injectMuleContextIfRequired(T object)
    {
        if (object instanceof MuleContextAware)
        {
            ((MuleContextAware)object).setMuleContext(this.muleContextDelegate);
        }
        return object;
    }

    public MuleContext getMuleRealContext()
    {
        return muleContext;
    }
}
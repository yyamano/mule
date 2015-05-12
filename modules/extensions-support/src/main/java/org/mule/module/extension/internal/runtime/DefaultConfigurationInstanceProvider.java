/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.MessageExchangePattern.REQUEST_RESPONSE;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.NamedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

/**
 * A {@link ConfigurationInstanceProvider} for returning instances that implement
 * a {@link Configuration}. Those instances are created through the
 * {@link Configuration#getInstantiator()} component.
 *
 * It supports both static and dynamic configurations (understanding by static
 * that non of its parameters have expressions, and dynamic that at least one of them does).
 * <p/>
 * In the case of static configurations, it will always return the same instance, in the case of
 * dynamic, it will evaluate those expressions and only return the same instance for equivalent
 * instances of {@link ResolverSetResult}.
 * <p/>
 * A {@link ResolverSet} is used for evaluating the attributes and creating new instances.
 * It also implements {@link NamedObject} since configurations are named and unique from a user's
 * point of view. Notice however that the named object is this provider and in the case of
 * dynamic configurations instances are not likely to be unique
 * <p/>
 * The generated instance will be registered with the {@code extensionManager}
 * through {@link ExtensionManagerAdapter#registerConfigurationInstance(Configuration, String, Object)}
 *
 *
 * @since 3.7.0
 */
public class DefaultConfigurationInstanceProvider implements ConfigurationInstanceProvider<Object>
{

    private final ConfigurationInstanceProvider<Object> delegate;

    public DefaultConfigurationInstanceProvider(String name,
                                                Configuration configuration,
                                                ResolverSet resolverSet,
                                                ExtensionManagerAdapter extensionManager,
                                                MuleContext muleContext)
    {
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);

        if (resolverSet.isDynamic())
        {
            delegate = new DynamicConfigurationInstanceProvider(name, configuration, configurationObjectBuilder, resolverSet, extensionManager);
        }
        else
        {
            try
            {
                Object config = configurationObjectBuilder.build(getInitialiserEvent(muleContext));
                extensionManager.registerConfigurationInstance(configuration, name, config);
                delegate = new StaticConfigurationInstanceProvider<>(configuration, config);
            }
            catch (MuleException e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    @Override
    public Object get(OperationContext operationContext)
    {
        return delegate.get(operationContext);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration()
    {
        return delegate.getConfiguration();
    }

    private MuleEvent getInitialiserEvent(MuleContext muleContext)
    {
        return new DefaultMuleEvent(new DefaultMuleMessage(null, muleContext), REQUEST_RESPONSE, (FlowConstruct) null);
    }

    private class StaticConfigurationInstanceProvider<T> implements ConfigurationInstanceProvider<T>
    {

        private final Configuration configuration;
        private final T configurationInstance;

        public StaticConfigurationInstanceProvider(Configuration configuration, T configurationInstance)
        {
            this.configuration = configuration;
            this.configurationInstance = configurationInstance;
        }

        @Override
        public T get(OperationContext operationContext)
        {
            return configurationInstance;
        }

        @Override
        public Configuration getConfiguration()
        {
            return configuration;
        }
    }
}

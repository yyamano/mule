/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleRuntimeException;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.resolver.ResolverSetResult;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A {@link ConfigurationInstanceProvider} which continuously evaluates the same
 * {@link ResolverSet} and then uses the resulting {@link ResolverSetResult}
 * to build an instance of a given type.
 * <p/>
 * Although each invocation to {@link #get(OperationContext)} is guaranteed to end up
 * in an invocation to {@link #resolverSet#resolve(MuleEvent)}, the resulting
 * {@link ResolverSetResult} might not end up generating a new instance. This is so because
 * {@link ResolverSetResult} instances are put in a cache to
 * guarantee that equivalent evaluations of the {@code resolverSet} return the same
 * instance.
 * <p/>
 * The generated instances will be registered with the {@link #extensionManager}
 * through {@link ExtensionManagerAdapter#registerConfigurationInstance(Configuration, String, Object)}
 *
 * @since 3.7.0
 */
public class DynamicConfigurationInstanceProvider implements ConfigurationInstanceProvider<Object>
{

    private final String name;
    private final Configuration configuration;
    private final ConfigurationObjectBuilder configurationObjectBuilder;
    private final ResolverSet resolverSet;
    private final ExtensionManagerAdapter extensionManager;

    private LoadingCache<ResolverSetResult, Object> cache;

    /**
     * Creates a new instance
     *
     * @param name                       the name of the config definition
     * @param configuration              the {@link Configuration} model
     * @param configurationObjectBuilder the introspection model of the objects this resolver produces
     * @param resolverSet                the {@link ResolverSet} that's going to be evaluated
     * @param muleContext                the current {@link MuleContext}
     */
    public DynamicConfigurationInstanceProvider(String name,
                                                Configuration configuration,
                                                ConfigurationObjectBuilder configurationObjectBuilder,
                                                ResolverSet resolverSet,
                                                ExtensionManagerAdapter extensionManager)
    {
        this.name = name;
        this.configuration = configuration;
        this.configurationObjectBuilder = configurationObjectBuilder;
        this.resolverSet = resolverSet;
        this.extensionManager = extensionManager;
        buildCache();
    }

    private void buildCache()
    {
        cache = CacheBuilder.newBuilder().build(new CacheLoader<ResolverSetResult, Object>()
        {
            @Override
            public Object load(ResolverSetResult key) throws Exception
            {
                Object config = configurationObjectBuilder.build(key);
                extensionManager.registerConfigurationInstance(configuration, name, config);

                return config;
            }
        });
    }

    /**
     * Evaluates {@link #resolverSet} using the given {@code event} and returns
     * an instance produced with the result. For equivalent {@link ResolverSetResult}s
     * it will return the same instance, for as long as the {@code expirationInterval} and
     * {@code expirationTimeUnit} were specified in the constructor
     *
     * @param event a {@link MuleEvent}
     * @return the resolved value
     */
    @Override
    public Object get(OperationContext operationContext)
    {
        validateOperationContext(operationContext);

        try
        {
            ResolverSetResult result = resolverSet.resolve(((DefaultOperationContext) operationContext).getEvent());
            return cache.getUnchecked(result);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    private void validateOperationContext(OperationContext operationContext)
    {
        checkArgument(operationContext != null, "operationContext cannot be null");
        if (!(operationContext instanceof DefaultOperationContext))
        {
            throw new IllegalArgumentException(String.format("operationContext was expected to be an instance of %s but got %s instead",
                                                             DefaultOperationContext.class.getName(), operationContext.getClass().getName()));
        }
    }
}

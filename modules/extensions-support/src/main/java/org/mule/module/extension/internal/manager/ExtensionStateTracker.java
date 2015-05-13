/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.introspection.Configuration;
import org.mule.extension.introspection.Extension;
import org.mule.extension.introspection.Operation;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.extension.runtime.OperationExecutor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;

/**
 * Holds state regarding the use that the platform is doing of a
 * certain {@link Extension}
 *
 * @since 3.7.0
 */
final class ExtensionStateTracker
{

    private final LoadingCache<Configuration, ConfigurationStateTracker> configurationsStates = CacheBuilder.newBuilder()
            .build(new CacheLoader<Configuration, ConfigurationStateTracker>()
            {
                @Override
                public ConfigurationStateTracker load(Configuration configuration) throws Exception
                {
                    return new ConfigurationStateTracker();
                }
            });

    <C> void registerConfigurationInstanceProvider(Configuration configuration,
                                                   String providerName,
                                                   ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {

        getStateTracker(configuration).registerInstanceProvider(providerName, configurationInstanceProvider);
    }

    <C> boolean isConfigurationInstanceProviderRegistered(ConfigurationInstanceProvider<C> configurationInstanceProvider)
    {
        return getStateTracker(configurationInstanceProvider.getConfiguration()).isInstanceProviderRegistered(configurationInstanceProvider);
    }

    <C> C getConfigurationInstance(String instanceProviderName, OperationContext operationContext) {
        //return getStateTracker(operationContext.getConfiguration()).getConfigurationInstance(instanceProviderName, operationContext);
        return null;
    }

    Map<String, ConfigurationInstanceProvider> getConfigurationInstanceProviders(Configuration configuration) {
        return ImmutableMap.copyOf(getStateTracker(configuration).getConfigurationInstanceProviders());
    }

    /**
     * Registers a {@code configurationInstance} which is a realization of a {@link Configuration}
     * model defined by {@code configuration}
     *
     * @param configuration         a {@link Configuration}
     * @param instanceName          the name of the instance
     * @param configurationInstance an instance which is compliant with the {@code configuration} model
     * @param <C>                   the type of the configuration instance
     */
    <C> void registerConfigurationInstance(Configuration configuration, String instanceName, C configurationInstance)
    {
        getStateTracker(configuration).registerInstance(instanceName, configurationInstance);
    }

    /**
     * Returns an {@link OperationExecutor} that was previously registered
     * through {@link #registerOperationExecutor(Configuration, Operation, Object, OperationExecutor)}
     *
     * @param configuration         a {@link Configuration}
     * @param operation             a {@link Operation} model
     * @param configurationInstance a previously registered configuration instance
     * @param <C>                   the type of the configuration instance
     * @return a {@link OperationExecutor}
     */
    <C> OperationExecutor getOperationExecutor(Configuration configuration, C configurationInstance, OperationContext operationContext)
    {
        return getStateTracker(configuration).getOperationExecutor(configurationInstance, operationContext);
    }

    /**
     * Registers a {@link OperationExecutor} for the {@code operation}|{@code configurationInstance}
     * pair.
     *
     * @param configuration         a {@link Configuration}
     * @param operation             a {@link Operation} model
     * @param configurationInstance a previously registered configuration instance
     * @param executor              a {@link OperationExecutor}
     * @param <C>                   the type of the configuration instance
     */
    <C> void registerOperationExecutor(Configuration configuration, Operation operation, C configurationInstance, OperationExecutor executor)
    {
        getStateTracker(configuration).registerOperationExecutor(operation, configurationInstance, executor);
    }

    private ConfigurationStateTracker getStateTracker(Configuration configuration) {
        return configurationsStates.getUnchecked(configuration);
    }
}

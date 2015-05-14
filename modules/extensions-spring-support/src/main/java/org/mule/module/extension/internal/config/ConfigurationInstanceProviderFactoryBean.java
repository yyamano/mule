/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.getResolverSet;
import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.ConfigurationObjectBuilder;
import org.mule.module.extension.internal.runtime.DynamicConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.StaticConfigurationInstanceProvider;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which returns a {@link ConfigurationInstanceProvider} that provides the actual instances
 * that implement a given {@link Configuration}. Subsequent invokations to {@link #getObject()} method
 * returns always the same {@link ConfigurationInstanceProvider}.
 *
 * @since 3.7.0
 */
final class ConfigurationInstanceProviderFactoryBean implements FactoryBean<ConfigurationInstanceProvider<Object>>
{

    private final ConfigurationInstanceProvider<Object> configurationInstanceProvider;

    ConfigurationInstanceProviderFactoryBean(String name,
                                             Configuration configuration,
                                             ElementDescriptor element)
    {
        ResolverSet resolverSet = getResolverSet(element, configuration.getParameters());
        ConfigurationObjectBuilder configurationObjectBuilder = new ConfigurationObjectBuilder(configuration, resolverSet);

        if (resolverSet.isDynamic())
        {
            configurationInstanceProvider = new DynamicConfigurationInstanceProvider(name, configuration, configurationObjectBuilder, resolverSet);
        }
        else
        {
            configurationInstanceProvider = new StaticConfigurationInstanceProvider<>(name, configuration, configurationObjectBuilder);
        }
    }

    @Override
    public ConfigurationInstanceProvider<Object> getObject() throws Exception
    {
        return configurationInstanceProvider;
    }

    /**
     * @return {@link ConfigurationInstanceProvider}
     */
    @Override
    public Class<ConfigurationInstanceProvider> getObjectType()
    {
        return ConfigurationInstanceProvider.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}

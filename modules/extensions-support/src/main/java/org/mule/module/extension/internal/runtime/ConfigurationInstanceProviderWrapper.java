/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.introspection.Configuration;
import org.mule.extension.runtime.ConfigurationInstanceProvider;
import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.manager.ExtensionManagerAdapter;

public class ConfigurationInstanceProviderWrapper<T> implements ConfigurationInstanceProvider<T>
{
    private final ConfigurationInstanceProvider<T> delegate;
    private final ExtensionManagerAdapter extensionManager;

    public ConfigurationInstanceProviderWrapper(ConfigurationInstanceProvider<T> delegate, ExtensionManagerAdapter extensionManager)
    {
        this.delegate = delegate;
        this.extensionManager = extensionManager;
    }

    @Override
    public T get(OperationContext operationContext)
    {
        T configurationInstance = delegate.get(operationContext);
        extensionManager.registerConfigurationInstance(delegate.getConfiguration(), delegate.getName(), configurationInstance);

        return configurationInstance;
    }

    @Override
    public Configuration getConfiguration()
    {
        return delegate.getConfiguration();
    }

    @Override
    public String getName()
    {
        return delegate.getName();
    }
}

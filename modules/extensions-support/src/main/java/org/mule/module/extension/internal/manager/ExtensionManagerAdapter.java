/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.manager;

import org.mule.extension.ExtensionManager;
import org.mule.extension.introspection.Configuration;

public interface ExtensionManagerAdapter extends ExtensionManager
{

    /**
     * Registers a {@code configurationInstance} which is an instance of an object which is compliant
     * with the {@link Configuration} modeled by {@code configuration}. It is mandatory for configuration
     * instances to be registered through this method before they can be used to execute operations.
     * Implementations of this method are to be considered thread-safe.
     *
     * @param configuration             a {@link Configuration} model
     * @param configurationInstanceName the name of the instance to be registered
     * @param configurationInstance     an object which is compliant with the {@code configuration} model
     * @param <C>                       the type of the configuration instance
     * @throws IllegalStateException if an instance with the same {@code configurationInstanceName} has already been registered
     */
    <C> void registerConfigurationInstance(Configuration configuration, String configurationInstanceName, C configurationInstance);
}

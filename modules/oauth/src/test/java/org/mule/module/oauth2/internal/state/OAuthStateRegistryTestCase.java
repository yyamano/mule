/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.state;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class OAuthStateRegistryTestCase extends AbstractMuleTestCase
{

    public static final String TEST_CONFIG_NAME = "testConfigName";

    @Test
    public void nonRegisteredConfigReturnsNull()
    {
        assertThat(new OAuthStateRegistry().getStateForConfig(TEST_CONFIG_NAME), nullValue());
    }

    @Test
    public void registerConfig()
    {
        final OAuthStateRegistry oAuthStateRegistry = new OAuthStateRegistry();
        final ConfigOAuthState registeredConfigState = new ConfigOAuthState();
        oAuthStateRegistry.registerOAuthState(TEST_CONFIG_NAME, registeredConfigState);
        assertThat(oAuthStateRegistry.getStateForConfig(TEST_CONFIG_NAME), is(registeredConfigState));
    }

    @Test
    public void unregisterConfig()
    {
        final OAuthStateRegistry oAuthStateRegistry = new OAuthStateRegistry();
        final ConfigOAuthState registeredConfigState = new ConfigOAuthState();
        oAuthStateRegistry.registerOAuthState(TEST_CONFIG_NAME, registeredConfigState);
        oAuthStateRegistry.unregisterOAuthState(TEST_CONFIG_NAME);
        assertThat(oAuthStateRegistry.getStateForConfig(TEST_CONFIG_NAME), nullValue());
    }

}

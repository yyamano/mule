/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.state;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class ConfigOAuthStateTestCase extends AbstractMuleTestCase
{

    public static final String USER_ID = "user";

    @Test
    public void nonExistentUserIdReturnNewConfig()
    {
        assertThat(new ConfigOAuthState().getStateForUser(USER_ID), notNullValue());
    }

    @Test
    public void existentUserIdReturnsPreviousConfig()
    {
        final ConfigOAuthState configOAuthState = new ConfigOAuthState();
        final UserOAuthState userState = configOAuthState.getStateForUser(USER_ID);
        assertThat(configOAuthState.getStateForUser(USER_ID), is(userState));
    }

}

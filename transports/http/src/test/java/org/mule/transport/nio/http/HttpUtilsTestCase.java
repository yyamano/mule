/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.nio.http;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class HttpUtilsTestCase
{
    private static final String TEST_DEFAULT_CHARSET = "DEFAULT_CS";

    @Test
    public void testExtractCharset()
    {
        assertThat(HttpUtils.extractCharset(null, TEST_DEFAULT_CHARSET), is(TEST_DEFAULT_CHARSET));
        assertThat(HttpUtils.extractCharset("", TEST_DEFAULT_CHARSET), is(TEST_DEFAULT_CHARSET));
        assertThat(HttpUtils.extractCharset("text/xml", TEST_DEFAULT_CHARSET), is(TEST_DEFAULT_CHARSET));
        assertThat(HttpUtils.extractCharset("text/xml; charset=utf-8", TEST_DEFAULT_CHARSET), is("utf-8"));
        assertThat(
            HttpUtils.extractCharset("text/xml; foo=bar; charset=utf-16; noise", TEST_DEFAULT_CHARSET),
            is("utf-16"));
    }
}

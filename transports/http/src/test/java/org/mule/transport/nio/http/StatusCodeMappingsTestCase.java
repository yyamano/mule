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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.RoutingException;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.ExceptionHelper;
import org.mule.config.i18n.MessageFactory;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

public class StatusCodeMappingsTestCase extends AbstractMuleTestCase
{
    @Test
    public void testErrorMappings() throws InitialisationException, ConfigurationException
    {
        MuleContext ctx = new DefaultMuleContextFactory().createMuleContext();
        String code = ExceptionHelper.getErrorMapping(HttpConnector.HTTP, RoutingException.class, ctx);
        assertEquals("500", code);

        code = ExceptionHelper.getErrorMapping(HttpConnector.HTTP, org.mule.api.security.SecurityException.class, ctx);
        assertEquals("403", code);

        code = ExceptionHelper.getErrorMapping(HttpConnector.HTTP, UnauthorisedException.class, ctx);
        assertEquals("401", code);

        code = ExceptionHelper.getErrorMapping("blah", DefaultMuleException.class, ctx);
        assertEquals(
            String.valueOf(new DefaultMuleException(MessageFactory.createStaticMessage("test")).getExceptionCode()),
            code);
    }

    // TODO reactivate when HTTPS support is added
    // @Test
    // public void testHttpsErrorMappings()
    // {
    // String code = ExceptionHelper.getErrorMapping("httpS",
    // RoutingException.class);
    // assertEquals("500", code);
    //
    // code = ExceptionHelper.getErrorMapping("HTTPS",
    // org.mule.api.security.SecurityException.class);
    // assertEquals("403", code);
    //
    // code = ExceptionHelper.getErrorMapping("https", UnauthorisedException.class);
    // assertEquals("401", code);
    // }
}

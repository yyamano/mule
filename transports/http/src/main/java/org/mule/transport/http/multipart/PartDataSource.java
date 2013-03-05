/*
 * $Id: PartDataSource.java 20320 2010-11-24 15:03:31Z dfeist $
 * -------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

/**
 * A {@link DataSource} for {@link Part}s.
 */
public class PartDataSource implements DataSource
{
    private final Part part;

    public PartDataSource(final Part part)
    {
        this.part = part;
    }

    public InputStream getInputStream() throws IOException
    {
        return part.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException
    {
        throw new UnsupportedOperationException("getOutputStream");
    }

    public String getContentType()
    {
        return part.getContentType();
    }

    public String getName()
    {
        return part.getName();
    }

    public Part getPart()
    {
        return part;
    }
}

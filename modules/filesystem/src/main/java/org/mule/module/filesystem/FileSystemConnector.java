/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem;

import org.mule.api.MuleException;

import java.util.List;

public interface FileSystemConnector
{

    public FileSystemParams getFileSystemParams();

    public List<FileSystemEntry> listFiles(FileSystemSearchCriteria searchCriteira, FileSystemParams params)
        throws MuleException;

}

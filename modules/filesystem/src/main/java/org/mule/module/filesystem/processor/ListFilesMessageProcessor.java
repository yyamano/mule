/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.module.filesystem.FileSystemParams;
import org.mule.module.filesystem.FileSystemSearchCriteria;

public class ListFilesMessageProcessor extends AbstractFileSystemMessageProcessor
{

    private FileSystemSearchCriteria searchCriteria;
    
    @Override
    protected MuleEvent doProcess(MuleEvent event, FileSystemParams params)
        throws MuleException
    {
        event.getMessage().setPayload(this.connector.listFiles(this.searchCriteria, params));
        return event;
    }

    public void setSearchCriteria(FileSystemSearchCriteria searchCriteria)
    {
        this.searchCriteria = searchCriteria;
    }
}

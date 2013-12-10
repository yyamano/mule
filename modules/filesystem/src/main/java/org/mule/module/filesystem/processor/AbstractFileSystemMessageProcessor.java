/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.filesystem.FileSystemConnector;
import org.mule.module.filesystem.FileSystemParams;
import org.mule.module.filesystem.FileSystemModuleUtils;

public abstract class AbstractFileSystemMessageProcessor implements MessageProcessor
{

    private static final String DEFAULT_TARGET = "#[payload]";

    protected FileSystemConnector connector;
    private String target = DEFAULT_TARGET;
    private FileSystemParams fileSystemParms;

    @Override
    public final MuleEvent process(MuleEvent event) throws MuleException
    {
        FileSystemParams params = FileSystemModuleUtils.merge(this.fileSystemParms, this.connector.getFileSystemParams());
        MuleEvent resultEvent = this.doProcess(event, params);
        event.getMuleContext()
            .getExpressionManager()
            .enrich(this.target, event, resultEvent.getMessage().getPayload());

        return resultEvent;
    }

    protected abstract MuleEvent doProcess(MuleEvent event, FileSystemParams params) throws MuleException;

    public void setConnector(FileSystemConnector connector)
    {
        this.connector = connector;
    }
    
    public void setFileSystemParms(FileSystemParams fileSystemParms)
    {
        this.fileSystemParms = fileSystemParms;
    }
}

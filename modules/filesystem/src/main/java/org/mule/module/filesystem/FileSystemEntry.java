/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.filesystem;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class FileSystemEntry implements Serializable
{

    private static final long serialVersionUID = -3251300599209830933L;
    
    private String name;
    private String path;
    private FileSystemEntryType type;
    private List<FileSystemEntry> childs = new LinkedList<FileSystemEntry>();
    private long size;
    private String group;
    private String user;
    private String symbolicLinkTarget;
    private Calendar timestamp;
    
    public String getName()
    {
        return name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public FileSystemEntryType getType()
    {
        return type;
    }
    public void setType(FileSystemEntryType type)
    {
        this.type = type;
    }
    public long getSize()
    {
        return size;
    }
    public void setSize(long size)
    {
        this.size = size;
    }
    public String getGroup()
    {
        return group;
    }
    public void setGroup(String group)
    {
        this.group = group;
    }
    public String getUser()
    {
        return user;
    }
    public void setUser(String user)
    {
        this.user = user;
    }
    public String getSymbolicLinkTarget()
    {
        return symbolicLinkTarget;
    }
    public void setSymbolicLinkTarget(String symbolicLinkTarget)
    {
        this.symbolicLinkTarget = symbolicLinkTarget;
    }
    public Calendar getTimestamp()
    {
        return timestamp;
    }
    public void setTimestamp(Calendar timestamp)
    {
        this.timestamp = timestamp;
    }
    public List<FileSystemEntry> getChilds()
    {
        return childs;
    }
    public void setChilds(List<FileSystemEntry> childs)
    {
        this.childs = childs;
    }
    public String getPath()
    {
        return path;
    }
    public void setPath(String path)
    {
        this.path = path;
    }
}
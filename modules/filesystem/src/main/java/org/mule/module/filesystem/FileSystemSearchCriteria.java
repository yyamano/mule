/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.filesystem;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class FileSystemSearchCriteria implements Serializable
{

    private static final long serialVersionUID = -3306223502175395913L;
    
    private boolean recursive = false;
    private boolean includeFolders = true;;
    private Long minFileAge;
    private TimeUnit minFileAgeTimeUnit = TimeUnit.MILLISECONDS;
    private Long maxFileAge;
    private TimeUnit maxFileAgeTimeUnit = TimeUnit.MILLISECONDS;
    private String filenameFilter;
    
    public boolean isRecursive()
    {
        return recursive;
    }
    public void setRecursive(boolean recursive)
    {
        this.recursive = recursive;
    }
    public Long getMinFileAge()
    {
        return minFileAge;
    }
    public void setMinFileAge(Long minFileAge)
    {
        this.minFileAge = minFileAge;
    }
    public Long getMaxFileAge()
    {
        return maxFileAge;
    }
    public void setMaxFileAge(Long maxFileAge)
    {
        this.maxFileAge = maxFileAge;
    }
    public String getFilenameFilter()
    {
        return filenameFilter;
    }
    public void setFilenameFilter(String filenameFilter)
    {
        this.filenameFilter = filenameFilter;
    }
    public boolean isIncludeFolders()
    {
        return includeFolders;
    }
    public void setIncludeFolders(boolean includeFolders)
    {
        this.includeFolders = includeFolders;
    }
    public TimeUnit getMinFileAgeTimeUnit()
    {
        return minFileAgeTimeUnit;
    }
    public void setMinFileAgeTimeUnit(TimeUnit minFileAgeTimeUnit)
    {
        this.minFileAgeTimeUnit = minFileAgeTimeUnit;
    }
    public TimeUnit getMaxFileAgeTimeUnit()
    {
        return maxFileAgeTimeUnit;
    }
    public void setMaxFileAgeTimeUnit(TimeUnit maxFileAgeTimeUnit)
    {
        this.maxFileAgeTimeUnit = maxFileAgeTimeUnit;
    }
}



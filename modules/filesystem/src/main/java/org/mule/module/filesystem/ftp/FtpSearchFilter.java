/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.ftp;

import org.mule.module.filesystem.FileSystemSearchCriteria;
import org.mule.transport.file.filters.FilenameWildcardFilter;

import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

public class FtpSearchFilter implements FTPFileFilter
{

    private final FileSystemSearchCriteria criteria;
    private FilenameWildcardFilter filenameFilter;

    public FtpSearchFilter(FileSystemSearchCriteria criteria)
    {
        this.criteria = criteria;
        String filenameExpression = criteria.getFilenameFilter();
        if (filenameExpression != null)
        {
            this.filenameFilter = new FilenameWildcardFilter(filenameExpression);
        }
    }

    @Override
    public boolean accept(FTPFile file)
    {
        if (file.isDirectory() && !this.criteria.isIncludeFolders()) {
            return false;
        }
        
        if (!this.evaluateFilename(file))
        {
            return false;
        }

        if (!this.evaluateMinFileAge(file))
        {
            return false;
        }

        if (!this.evaluateMaxFileAge(file))
        {
            return false;
        }

        return true;
    }

    private boolean evaluateFilename(FTPFile file)
    {
        if (this.filenameFilter != null)
        {
            return this.filenameFilter.accept(file.getName());
        }
        else
        {
            return true;
        }
    }

    private boolean evaluateMinFileAge(FTPFile file)
    {
        Long minFileAge = this.criteria.getMinFileAge();
        if (minFileAge != null)
        {
            return this.getFileAgeInMillis(file) >= this.criteria.getMinFileAgeTimeUnit()
                .toMillis(minFileAge);
        }
        else
        {
            return true;
        }
    }

    private boolean evaluateMaxFileAge(FTPFile file)
    {
        Long maxFileAge = this.criteria.getMaxFileAge();
        if (maxFileAge != null)
        {
            return this.getFileAgeInMillis(file) <= this.criteria.getMinFileAgeTimeUnit()
                .toMillis(maxFileAge);
        }
        else
        {
            return true;
        }
    }

    private long getFileAgeInMillis(FTPFile file)
    {
        Calendar timestamp = file.getTimestamp();
        Calendar threshold = Calendar.getInstance(timestamp.getTimeZone());
        return threshold.getTimeInMillis() - timestamp.getTimeInMillis();
    }

}

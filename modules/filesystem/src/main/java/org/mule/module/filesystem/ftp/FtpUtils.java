/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.filesystem.ftp;

import org.mule.module.filesystem.FileSystemConnector;
import org.mule.module.filesystem.FileSystemEntry;
import org.mule.module.filesystem.FileSystemEntryType;
import org.mule.module.filesystem.FileSystemParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.net.ftp.FTPFile;

public abstract class FtpUtils
{

    public static List<FileSystemEntry> parse(FTPFile[] files, String path)
    {
        if (files == null || files.length == 0)
        {
            return Collections.<FileSystemEntry> emptyList();
        }

        List<FileSystemEntry> entries = new ArrayList<FileSystemEntry>(files.length);
        for (FTPFile file : files)
        {
            entries.add(parse(file, path));
        }

        return entries;
    }

    public static FileSystemEntry parse(FTPFile file, String path)
    {
        FileSystemEntry entry = new FileSystemEntry();
        entry.setGroup(file.getGroup());
        entry.setName(file.getName());
        entry.setSize(file.getSize());
        entry.setSymbolicLinkTarget(file.getLink());
        entry.setTimestamp(file.getTimestamp());
        entry.setUser(entry.getUser());
        entry.setPath(path);

        FileSystemEntryType type;
        if (file.isDirectory())
        {
            type = FileSystemEntryType.FOLDER;
        }
        else if (file.isSymbolicLink())
        {
            type = FileSystemEntryType.SYMBOLIC_LINK;
        }
        else
        {
            type = FileSystemEntryType.FILE;
        }

        entry.setType(type);

        return entry;
    }

    public static String toKey(FileSystemParams params)
    {
        return String.format("%s@%s:%d", params.getUsername(), params.getHost(), params.getPort());
    }
}

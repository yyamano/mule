/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.filesystem;

import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;

public abstract class FileSystemModuleUtils
{

    @SuppressWarnings("unchecked")
    public static FileSystemParams merge(FileSystemParams main, FileSystemParams complement)
    {
        try
        {
            Map<String, Object> mainDescriptor = BeanUtils.describe(main);
            Map<String, Object> complementDescriptor = BeanUtils.describe(complement);

            for (Map.Entry<String, Object> entry : mainDescriptor.entrySet())
            {
                complementDescriptor.put(entry.getKey(), entry.getValue());
            }

            FileSystemParams merged = new FileSystemParams();
            BeanUtils.populate(merged, complementDescriptor);

            return merged;
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Could not merge configs", e);
        }
    }
}



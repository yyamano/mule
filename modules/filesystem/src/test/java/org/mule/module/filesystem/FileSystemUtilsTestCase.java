/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.filesystem;

import static junit.framework.Assert.assertEquals;

import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class FileSystemUtilsTestCase
{

    @Test
    public void mergeParamAgainstEmptyOne() {
        FileSystemParams main = this.makeTestParams();
        FileSystemParams complement = new FileSystemParams();
        
        complement = FileSystemModuleUtils.merge(main, complement);
        
        assertEquals(main.getHost(), complement.getHost());
        assertEquals(main.getPassword(), complement.getPassword());
        assertEquals(main.getPath(), complement.getPath());
        assertEquals(main.getPort(), complement.getPort());
        assertEquals(main.getTargetPath(), complement.getTargetPath());
        assertEquals(main.getUsername(), complement.getUsername());
    }
    
    @Test
    public void mergeOverridingParamsProperties() {
        FileSystemParams main = new FileSystemParams();
        FileSystemParams complement = this.makeTestParams();
        
        final String user = "overriding user";
        final String password = "password that overrides all passwords";
        final String host = "cloudhub.io";
        
        main.setHost(host);
        main.setUsername(user);
        main.setPassword(password);
        
        complement = FileSystemModuleUtils.merge(main, complement);
        
        assertEquals(host, complement.getHost());
        assertEquals(password, complement.getPassword());
        assertEquals(main.getPath(), complement.getPath());
        assertEquals(main.getPort(), complement.getPort());
        assertEquals(main.getTargetPath(), complement.getTargetPath());
        assertEquals(user, complement.getUsername());
    }
    
    private FileSystemParams makeTestParams() {
        FileSystemParams params = new FileSystemParams();
        params.setUsername("mg");
        params.setPassword("mg1234");
        params.setPath("/~/Desktop");
        params.setPort(2110);
        params.setTargetPath("/target/");
        params.setHost("localhost");
        
        return params;
   }
}



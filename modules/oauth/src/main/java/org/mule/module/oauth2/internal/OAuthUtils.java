/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal;

public class OAuthUtils
{

    public final static String ACCESS_TOKEN_EXPRESSION = "#[regex('" + ".*\"access_token\"[ ]*:[ ]*\"([^\\\"]*)\".*" + "')]";
    public final static String REFRESH_TOKEN_EXPRESSION = "#[regex('" + ".*\"refresh_token\"[ ]*:[ ]*\"([^\\\"]*)\".*" + "')]";
    public final static String EXPIRATION_TIME_EXPRESSION = "#[regex('" + ".*\"expires_in\"[ ]*:[ ]*([\\\\d]*).*" + "')]";

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.module.launcher.artifact.ArtifactFactory;

import java.io.IOException;
import java.util.Collection;

/**
 *
 */
public interface DomainFactory extends ArtifactFactory<Domain>
{

    //TODO remove to a domain repository.
    public Domain createAppDomain(String appName) throws IOException;

}

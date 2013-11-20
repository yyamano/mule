/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.artifact;

import org.mule.api.MuleContext;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.InstallException;

import java.io.File;

public interface Artifact
{

    void install() throws InstallException;

    void init();

    void start() throws DeploymentStartException;

    void stop();

    void dispose();

    void redeploy();

    String getArtifactName();

    File[] getConfigResourcesFile();

    ArtifactClassLoader getArtifactClassLoader();

    MuleContext getMuleContext();
}

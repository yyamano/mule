package org.mule.module.launcher.artifact;

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

}

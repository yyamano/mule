package org.mule.module.launcher.artifact;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public interface ArtifactFactory<T extends Artifact>
{

    public T createArtifact(String artifactName) throws IOException;

    File getArtifactDir();

}

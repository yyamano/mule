package org.mule.module.launcher.artifact;

import java.net.URL;

public interface ArtifactClassLoader
{
    String getArtifactName();

    URL findResource(String s);

    /**
     * Unfortunately ClassLoader is an abstract class and not an interface so in
     * order to allow a class loader to extend any subclass of ClassLoader this method
     * is required. Most implementations will return this.
     *
     * @return the class loader represented by this class.
     */
    ClassLoader getClassLoader();
}

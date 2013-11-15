package org.mule.module.launcher.domain;

import org.mule.api.MuleContext;
import org.mule.module.launcher.artifact.Artifact;

/**
 *
 */
public interface Domain extends Artifact
{

    boolean containsSharedResources();

    Object getContext();

    MuleContext getMuleContext();

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.runner.api;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.petstore.extension.PetStoreConnector;

import com.google.common.io.PatternFilenameFilter;

import java.io.File;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ExtensionPluginMetadataGeneratorTestCase {

  private static final String META_INF = "META-INF";

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private Artifact heisenbergPlugin = new DefaultArtifact("org.mule.tests:mule-heisenberg-extension:1.0-SNAPSHOT");
  private Artifact petStorePlugin = new DefaultArtifact("org.mule.tests:mule-petstore-extension:1.0-SNAPSHOT");

  private ExtensionPluginMetadataGenerator generator;

  @Before
  public void before() throws Exception {
    generator = new ExtensionPluginMetadataGenerator(temporaryFolder.newFolder());
  }

  @Test
  public void scanningClassPathShouldNotIncludeSpringStuff() {
    Class scanned = generator.scanForExtensionAnnotatedClasses(heisenbergPlugin, newArrayList(
                                                                                              this.getClass()
                                                                                                  .getProtectionDomain()
                                                                                                  .getCodeSource()
                                                                                                  .getLocation()));

    assertThat(scanned, is(nullValue()));
  }

  @Test
  public void generateExtensionManifestForTwoExtensionsInDifferentFolders() {
    File heisenbergPluginFolder = generator.generateExtensionManifest(heisenbergPlugin, HeisenbergExtension.class);
    File petStorePluginFolder = generator.generateExtensionManifest(petStorePlugin, PetStoreConnector.class);

    assertThat(heisenbergPluginFolder, not(equalTo(petStorePluginFolder)));
  }

  @Test
  public void generateExtensionMetadataForTwoExtensionsInDifferentFolders() throws Exception {
    File heisenbergPluginFolder = generator.generateExtensionManifest(heisenbergPlugin, HeisenbergExtension.class);
    File petStorePluginFolder = generator.generateExtensionManifest(petStorePlugin, PetStoreConnector.class);

    generator.generateDslResources();

    assertThat(listFiles(heisenbergPluginFolder, "heisenberg.xsd"), arrayWithSize(1));
    assertThat(listFiles(heisenbergPluginFolder, "petstore.xsd"), arrayWithSize(0));

    assertThat(listFiles(petStorePluginFolder, "heisenberg.xsd"), arrayWithSize(0));
    assertThat(listFiles(petStorePluginFolder, "petstore.xsd"), arrayWithSize(1));
  }

  public String[] listFiles(File pluginResourcesFolder, String pattern) {
    return new File(pluginResourcesFolder, META_INF).list(new PatternFilenameFilter(pattern));
  }

}
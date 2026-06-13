/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.spark.singularity.maven;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

@org.testng.annotations.Test(groups = "unit")
public class InstallSingularityMojoTest {

  private Path buildDirectory;

  @BeforeMethod
  public void createBuildDirectory ()
    throws Exception {

    buildDirectory = Files.createTempDirectory("install-singularity");
  }

  @AfterMethod(alwaysRun = true)
  public void deleteBuildDirectory () {

    MojoTestSupport.deleteTree(buildDirectory);
  }

  private InstallSingularityMojo mojo (String classifier, boolean skip, StubArtifactInstaller installer)
    throws Exception {

    MavenProject project = new MavenProject();
    Build build = new Build();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");
    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);
    project.setArtifact(new DefaultArtifact("org.smallmind.test", "sample-app", "1.0.0", "compile", "jar", classifier, new DefaultArtifactHandler("jar")));

    InstallSingularityMojo mojo = new InstallSingularityMojo();

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "localRepository", null);
    MojoTestSupport.setField(mojo, "skip", skip);
    MojoTestSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());
    MojoTestSupport.setField(mojo, "artifactInstaller", installer);

    return mojo;
  }

  private static boolean hasSignatureMetadata (org.apache.maven.artifact.Artifact artifact) {

    for (ArtifactMetadata metadata : artifact.getMetadataList()) {
      if (metadata instanceof AscArtifactMetadata) {

        return true;
      }
    }

    return false;
  }

  public void testInstallsTheSingularityJar ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo(null, false, installer).execute();

    Assert.assertEquals(installer.installedFile(), buildDirectory.resolve("sample-app-1.0.0.jar").toFile());
    Assert.assertFalse(hasSignatureMetadata(installer.installedArtifact()));
  }

  public void testAttachesTheSignatureWhenAnAscFileIsPresent ()
    throws Exception {

    Files.writeString(buildDirectory.resolve("sample-app-1.0.0.jar.asc"), "signature", StandardCharsets.UTF_8);

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo(null, false, installer).execute();

    Assert.assertTrue(hasSignatureMetadata(installer.installedArtifact()));
  }

  public void testClassifierIsReflectedInTheJarNameAndArtifact ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo("shaded", false, installer).execute();

    Assert.assertEquals(installer.installedFile(), buildDirectory.resolve("sample-app-1.0.0-shaded.jar").toFile());
    Assert.assertEquals(installer.installedArtifact().getClassifier(), "shaded");
  }

  public void testSkipInstallsNothing ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo(null, true, installer).execute();

    Assert.assertNull(installer.installedFile());
  }

  public void testInstallerFailureIsWrapped ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(null, false, new StubArtifactInstaller(true)).execute());
  }
}

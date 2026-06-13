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
package org.smallmind.spark.tanukisoft.maven;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.model.Build;
import org.testng.Assert;

@org.testng.annotations.Test(groups = "unit")
public class InstallWrapperMojoTest {

  private static final String BUILD_DIRECTORY = "/build/target";

  private InstallWrapperMojo mojo (String classifier, boolean skip, String compression, StubArtifactInstaller installer)
    throws Exception {

    MavenProject project = new MavenProject();
    Build build = new Build();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");
    build.setDirectory(BUILD_DIRECTORY);
    project.setBuild(build);

    InstallWrapperMojo mojo = new InstallWrapperMojo();

    TanukiMojoSupport.setField(mojo, "project", project);
    TanukiMojoSupport.setField(mojo, "localRepository", null);
    TanukiMojoSupport.setField(mojo, "applicationName", "sample-app");
    TanukiMojoSupport.setField(mojo, "classifier", classifier);
    TanukiMojoSupport.setField(mojo, "compression", compression);
    TanukiMojoSupport.setField(mojo, "skip", skip);
    TanukiMojoSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());
    TanukiMojoSupport.setField(mojo, "artifactInstaller", installer);

    return mojo;
  }

  public void testInstallsTheAggregateArchiveWithTheAppClassifier ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo(null, false, "zip", installer).execute();

    Assert.assertEquals(installer.installedFile(), new File(BUILD_DIRECTORY, "sample-app-1.0.0-app.zip"));
    Assert.assertEquals(installer.installedArtifact().getClassifier(), "app");
    Assert.assertEquals(installer.installedArtifact().getType(), "zip");
  }

  public void testClassifierIsFoldedIntoBothTheNameAndTheArtifactClassifier ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo("nightly", false, "zip", installer).execute();

    Assert.assertEquals(installer.installedFile(), new File(BUILD_DIRECTORY, "sample-app-1.0.0-nightly-app.zip"));
    Assert.assertEquals(installer.installedArtifact().getClassifier(), "nightly-app");
  }

  public void testSkipInstallsNothing ()
    throws Exception {

    StubArtifactInstaller installer = new StubArtifactInstaller();

    mojo(null, true, "zip", installer).execute();

    Assert.assertNull(installer.installedFile());
  }

  public void testUnknownCompressionIsReported ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(null, false, "rar", new StubArtifactInstaller()).execute());
  }

  public void testInstallerFailureIsWrapped ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(null, false, "zip", new StubArtifactInstaller(true)).execute());
  }
}

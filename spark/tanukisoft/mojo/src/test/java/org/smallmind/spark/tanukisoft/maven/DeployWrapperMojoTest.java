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
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;

@org.testng.annotations.Test(groups = "unit")
public class DeployWrapperMojoTest {

  private static final String BUILD_DIRECTORY = "/build/target";

  private DeployWrapperMojo mojo (boolean skip, String compression, StubArtifactDeployer deployer)
    throws Exception {

    MavenProject project = new MavenProject();
    Build build = new Build();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");
    build.setDirectory(BUILD_DIRECTORY);
    project.setBuild(build);

    DeployWrapperMojo mojo = new DeployWrapperMojo();

    TanukiMojoSupport.setField(mojo, "project", project);
    TanukiMojoSupport.setField(mojo, "localRepository", null);
    TanukiMojoSupport.setField(mojo, "deploymentRepository", null);
    TanukiMojoSupport.setField(mojo, "applicationName", "sample-app");
    TanukiMojoSupport.setField(mojo, "classifier", null);
    TanukiMojoSupport.setField(mojo, "compression", compression);
    TanukiMojoSupport.setField(mojo, "skip", skip);
    TanukiMojoSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());
    TanukiMojoSupport.setField(mojo, "artifactDeployer", deployer);

    return mojo;
  }

  public void testDeploysTheAggregateArchiveWithTheAppClassifier ()
    throws Exception {

    StubArtifactDeployer deployer = new StubArtifactDeployer();

    mojo(false, "zip", deployer).execute();

    Assert.assertEquals(deployer.deployedFile(), new File(BUILD_DIRECTORY, "sample-app-1.0.0-app.zip"));
    Assert.assertEquals(deployer.deployedArtifact().getClassifier(), "app");
  }

  public void testSkipDeploysNothing ()
    throws Exception {

    StubArtifactDeployer deployer = new StubArtifactDeployer();

    mojo(true, "zip", deployer).execute();

    Assert.assertNull(deployer.deployedFile());
  }

  public void testUnknownCompressionIsReported ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(false, "rar", new StubArtifactDeployer()).execute());
  }

  public void testDeployerFailureIsWrapped ()
    throws Exception {

    Assert.assertThrows(MojoExecutionException.class, () -> mojo(false, "zip", new StubArtifactDeployer(true)).execute());
  }
}

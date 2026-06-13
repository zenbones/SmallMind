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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

// Fast branches of the goal that do not require a fully staged bundle; the full end-to-end build is covered by
// SingularityBundleIntegrationTest.
@org.testng.annotations.Test(groups = "unit")
public class GenerateSingularityMojoTest {

  private Path buildDirectory;

  @BeforeMethod
  public void createBuildDirectory ()
    throws Exception {

    buildDirectory = Files.createTempDirectory("singularity-mojo-unit");
  }

  @AfterMethod(alwaysRun = true)
  public void deleteBuildDirectory () {

    MojoTestSupport.deleteTree(buildDirectory);
  }

  private GenerateSingularityMojo mojo (boolean skip)
    throws Exception {

    MavenProject project = new MavenProject();
    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);

    GenerateSingularityMojo mojo = new GenerateSingularityMojo();

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "singularityBuildDir", "singularity");
    MojoTestSupport.setField(mojo, "skip", skip);
    MojoTestSupport.setField(mojo, "verbose", false);

    return mojo;
  }

  // skip short-circuits the whole goal: nothing is staged and no artifact is attached.
  public void testSkipLeavesTheProjectUntouched ()
    throws Exception {

    GenerateSingularityMojo mojo = mojo(true);

    mojo.execute();

    Assert.assertFalse(Files.exists(buildDirectory.resolve("singularity")));
  }

  // Without the boot dependency among the plugin's own artifacts there is nothing to bootstrap the bundle with.
  public void testMissingBootClassesIsReported ()
    throws Exception {

    GenerateSingularityMojo mojo = mojo(false);

    MojoTestSupport.setField(mojo, "pluginArtifacts", List.of());
    MojoTestSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());

    Assert.assertThrows(MojoExecutionException.class, mojo::execute);
  }
}

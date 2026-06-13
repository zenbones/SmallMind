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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.smallmind.spark.singularity.boot.SingularityEntryPoint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Exercises the secondary branches of {@code generate-singularity}: a configured exclusion (skipped) alongside an
 * included dependency, a project classifier folded into the output name, verbose logging enabled, and a project with
 * no {@code classes} output directory.
 */
@org.testng.annotations.Test(groups = "integration")
public class GenerateSingularityMojoBranchIntegrationTest {

  private Path workspace;
  private Path bundle;

  @BeforeClass
  public void buildBundle ()
    throws Exception {

    workspace = Files.createTempDirectory("singularity-branch-it");

    // Deliberately no "classes" directory is created beneath the build directory, to exercise the absent-output path.
    Path buildDirectory = Files.createDirectories(workspace.resolve("build"));
    Path includedDependency = workspace.resolve("included-1.0.jar");
    Path excludedDependency = workspace.resolve("excluded-1.0.jar");

    MojoTestSupport.buildJar(includedDependency, Map.of("org/example/Included.class", "included".getBytes(StandardCharsets.UTF_8)));
    MojoTestSupport.buildJar(excludedDependency, Map.of("org/example/Excluded.class", "excluded".getBytes(StandardCharsets.UTF_8)));

    Path bootLocation = Path.of(SingularityEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path bootJar = MojoTestSupport.bootClassesAsJar(bootLocation, workspace.resolve("spark-singularity-boot.jar"));

    MavenProject project = new MavenProject();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");

    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);
    // A classifier on the project artifact is folded into the produced bundle name.
    project.setArtifact(new DefaultArtifact("org.smallmind.test", "sample-app", "1.0.0", "compile", "jar", "shaded", new DefaultArtifactHandler("jar")));

    DefaultArtifactHandler runtimeHandler = new DefaultArtifactHandler("jar");

    runtimeHandler.setAddedToClasspath(true);

    DefaultArtifact includedArtifact = new DefaultArtifact("org.smallmind.test", "included", "1.0", "runtime", "jar", null, runtimeHandler);
    DefaultArtifact excludedArtifact = new DefaultArtifact("org.smallmind.test", "excluded", "1.0", "runtime", "jar", null, runtimeHandler);

    includedArtifact.setFile(includedDependency.toFile());
    excludedArtifact.setFile(excludedDependency.toFile());
    project.setArtifacts(new java.util.LinkedHashSet<Artifact>(List.of(includedArtifact, excludedArtifact)));

    DefaultArtifact bootArtifact = new DefaultArtifact("org.smallmind", "spark-singularity-boot", "7.1.0-SNAPSHOT", "compile", "jar", null, new DefaultArtifactHandler("jar"));

    bootArtifact.setFile(bootJar.toFile());

    Exclusion exclusion = new Exclusion();

    exclusion.setGroupId("org.smallmind.test");
    exclusion.setArtifactId("excluded");

    GenerateSingularityMojo mojo = new GenerateSingularityMojo();

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "singularityBuildDir", "singularity");
    MojoTestSupport.setField(mojo, "mainClass", "org.smallmind.test.SampleApp");
    MojoTestSupport.setField(mojo, "skip", false);
    MojoTestSupport.setField(mojo, "verbose", true);
    MojoTestSupport.setField(mojo, "exclusions", new Exclusion[] {exclusion});
    MojoTestSupport.setField(mojo, "pluginArtifacts", List.of(bootArtifact));
    MojoTestSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());

    mojo.execute();

    bundle = buildDirectory.resolve("sample-app-1.0.0-shaded.jar");
  }

  @AfterClass(alwaysRun = true)
  public void deleteWorkspace () {

    MojoTestSupport.deleteTree(workspace);
  }

  public void testClassifierIsFoldedIntoTheBundleName () {

    Assert.assertTrue(Files.isRegularFile(bundle), "the classifier was not reflected in the produced bundle name");
  }

  public void testIncludedDependencyIsStagedButTheExcludedOneIsNot ()
    throws Exception {

    try (JarFile jarFile = new JarFile(bundle.toFile())) {

      JarEntry included = jarFile.getJarEntry("META-INF/singularity/lib/included-1.0.jar");
      JarEntry excluded = jarFile.getJarEntry("META-INF/singularity/lib/excluded-1.0.jar");

      Assert.assertNotNull(included, "the included dependency should be bundled");
      Assert.assertNull(excluded, "the excluded dependency should be skipped");
    }
  }

  public void testAttachedArtifactIsRegistered () {

    Assert.assertEquals(bundle.getFileName().toString(), "sample-app-1.0.0-shaded.jar");
  }
}

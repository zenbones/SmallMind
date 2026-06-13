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

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.smallmind.spark.singularity.boot.SingularityEntryPoint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * When a bundle names {@link SingularityEntryPoint} itself as its {@code Singularity-Class}, the bootstrap must
 * install the class loader but decline to re-dispatch to itself. The bundle is launched in a child JVM and is
 * expected to exit cleanly without the application side effect ever occurring.
 */
@org.testng.annotations.Test(groups = "integration")
public class SingularityEntryPointRecursionGuardIntegrationTest {

  private Path workspace;
  private Path bundle;

  @BeforeClass
  public void buildBundle ()
    throws Exception {

    workspace = Files.createTempDirectory("singularity-recursion-it");

    Path buildDirectory = Files.createDirectories(workspace.resolve("build"));

    Files.createDirectories(buildDirectory.resolve("classes"));

    Path bootLocation = Path.of(SingularityEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path bootJar = MojoTestSupport.bootClassesAsJar(bootLocation, workspace.resolve("spark-singularity-boot.jar"));

    MavenProject project = new MavenProject();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");

    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);
    project.setArtifact(new DefaultArtifact("org.smallmind.test", "sample-app", "1.0.0", "compile", "jar", null, new DefaultArtifactHandler("jar")));
    project.setArtifacts(java.util.Set.of());

    DefaultArtifact bootArtifact = new DefaultArtifact("org.smallmind", "spark-singularity-boot", "7.1.0-SNAPSHOT", "compile", "jar", null, new DefaultArtifactHandler("jar"));

    bootArtifact.setFile(bootJar.toFile());

    GenerateSingularityMojo mojo = new GenerateSingularityMojo();

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "singularityBuildDir", "singularity");
    // Naming the entry point itself as the application class is exactly the self-reference the bootstrap must ignore.
    MojoTestSupport.setField(mojo, "mainClass", SingularityEntryPoint.class.getName());
    MojoTestSupport.setField(mojo, "skip", false);
    MojoTestSupport.setField(mojo, "verbose", false);
    MojoTestSupport.setField(mojo, "exclusions", null);
    MojoTestSupport.setField(mojo, "pluginArtifacts", List.of(bootArtifact));
    MojoTestSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());

    mojo.execute();

    bundle = buildDirectory.resolve("sample-app-1.0.0.jar");
  }

  @AfterClass(alwaysRun = true)
  public void deleteWorkspace () {

    MojoTestSupport.deleteTree(workspace);
  }

  public void testSelfReferentialBundleBootsWithoutRecursing ()
    throws Exception {

    Path marker = workspace.resolve("marker.txt");
    List<String> command = new ArrayList<>();

    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());

    for (String inputArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (inputArgument.startsWith("-javaagent:") && inputArgument.contains("jacoco")) {
        command.add(inputArgument);
      }
    }

    command.add("-jar");
    command.add(bundle.toString());
    command.add(marker.toString());

    ProcessBuilder processBuilder = new ProcessBuilder(command);

    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    boolean finished = process.waitFor(60, TimeUnit.SECONDS);

    if (!finished) {
      process.destroyForcibly();
      Assert.fail("the bundle's child JVM did not exit within the timeout");
    }

    Assert.assertEquals(process.exitValue(), 0, "the self-referential bundle exited abnormally; output:\n" + output);
    Assert.assertFalse(Files.exists(marker), "the bootstrap must not dispatch to itself, so no application side effect should occur");
  }
}

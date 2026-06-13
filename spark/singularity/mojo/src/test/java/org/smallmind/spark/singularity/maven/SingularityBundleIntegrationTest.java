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
import java.util.concurrent.TimeUnit;
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
 * End-to-end coverage of the {@code generate-singularity} goal and the runtime it produces. The Mojo is driven with a
 * hand-built {@link MavenProject} to assemble a real bundle, which is then launched in a child JVM; the bundle's
 * {@link SingularityEntryPoint} must install the class loader, load the application class from the outer jar (the
 * {@code jar:} path) and a dependency class from a nested library jar (the {@code singularity:} path), and forward
 * the supplied arguments to the application's {@code main}.
 */
@org.testng.annotations.Test(groups = "integration")
public class SingularityBundleIntegrationTest {

  private static final String APP_CLASS = "org.smallmind.spark.singularity.app.EntryPointApp";
  private static final String DEP_CLASS = "org.smallmind.spark.singularity.dep.DepGreeter";

  private Path workspace;
  private Path bundle;
  private MavenProject project;

  @BeforeClass
  public void buildBundle ()
    throws Exception {

    workspace = Files.createTempDirectory("singularity-bundle-it");

    Path buildDirectory = Files.createDirectories(workspace.resolve("build"));
    Path classesDirectory = Files.createDirectories(buildDirectory.resolve("classes"));
    Path dependencyJar = workspace.resolve("dependency-greeter.jar");

    // The application class is laid down as a project class (served from the outer jar); the dependency class is
    // packaged only inside a runtime dependency jar (served from a nested library jar).
    MojoTestSupport.writeClassInto(classesDirectory, APP_CLASS);
    MojoTestSupport.buildJar(dependencyJar, Map.of(MojoTestSupport.resourcePath(DEP_CLASS), MojoTestSupport.classBytes(DEP_CLASS)));

    Path bootLocation = Path.of(SingularityEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    Path bootJar = MojoTestSupport.bootClassesAsJar(bootLocation, workspace.resolve("spark-singularity-boot.jar"));

    project = new MavenProject();
    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");

    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);
    project.setArtifact(new DefaultArtifact("org.smallmind.test", "sample-app", "1.0.0", "compile", "jar", null, new DefaultArtifactHandler("jar")));

    DefaultArtifactHandler runtimeHandler = new DefaultArtifactHandler("jar");

    runtimeHandler.setAddedToClasspath(true);

    DefaultArtifact dependencyArtifact = new DefaultArtifact("org.smallmind.test", "dependency-greeter", "1.0.0", "runtime", "jar", null, runtimeHandler);

    dependencyArtifact.setFile(dependencyJar.toFile());
    project.setArtifacts(Set.<Artifact>of(dependencyArtifact));

    DefaultArtifact bootArtifact = new DefaultArtifact("org.smallmind", "spark-singularity-boot", "7.1.0-SNAPSHOT", "compile", "jar", null, new DefaultArtifactHandler("jar"));

    bootArtifact.setFile(bootJar.toFile());

    GenerateSingularityMojo mojo = new GenerateSingularityMojo();

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "singularityBuildDir", "singularity");
    MojoTestSupport.setField(mojo, "mainClass", APP_CLASS);
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

  public void testGoalProducesAnAttachedExecutableBundle () {

    Assert.assertTrue(Files.isRegularFile(bundle), "the goal did not produce the expected bundle");
    Assert.assertEquals(project.getAttachedArtifacts().size(), 1);
    Assert.assertEquals(project.getAttachedArtifacts().get(0).getFile(), bundle.toFile());
  }

  public void testBundleBootsAndLoadsAcrossBothProtocols ()
    throws Exception {

    Path marker = workspace.resolve("marker.txt");
    String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();

    java.util.List<String> command = new java.util.ArrayList<>();

    command.add(javaExecutable);

    // When the test JVM is itself running under the JaCoCo agent, propagate the same agent (it appends to the shared
    // exec file) so the child's SingularityEntryPoint execution is recorded in coverage rather than being invisible.
    for (String inputArgument : java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments()) {
      if (inputArgument.startsWith("-javaagent:") && inputArgument.contains("jacoco")) {
        command.add(inputArgument);
      }
    }

    command.add("-jar");
    command.add(bundle.toString());
    command.add(marker.toString());
    command.add("hello");
    command.add("world");

    ProcessBuilder processBuilder = new ProcessBuilder(command);

    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    boolean finished = process.waitFor(60, TimeUnit.SECONDS);

    if (!finished) {
      process.destroyForcibly();
      Assert.fail("the bundle's child JVM did not exit within the timeout");
    }

    Assert.assertEquals(process.exitValue(), 0, "the bundle's child JVM exited abnormally; output:\n" + output);
    Assert.assertEquals(Files.readString(marker, StandardCharsets.UTF_8), "hello world|greeted-by-dependency");
  }
}

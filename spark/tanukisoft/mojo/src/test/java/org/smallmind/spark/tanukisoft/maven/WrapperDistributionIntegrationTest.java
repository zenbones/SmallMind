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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.project.MavenProject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * End-to-end coverage of the {@code generate-wrapper} goal. The Mojo is driven with a hand-built {@link MavenProject}
 * to stage a complete Tanuki wrapper distribution from the bundled binaries and Freemarker templates, then the
 * produced tree and aggregate archive are inspected. Both the Unix (script via template) and Windows (batch files)
 * style paths are exercised.
 */
@org.testng.annotations.Test(groups = "integration")
public class WrapperDistributionIntegrationTest {

  private static final String APP_NAME = "sample-app";
  private static final String VERSION = "1.0.0";

  private Path workspace;

  @BeforeMethod
  public void createWorkspace ()
    throws Exception {

    workspace = Files.createTempDirectory("wrapper-distribution-it");
  }

  @AfterMethod(alwaysRun = true)
  public void deleteWorkspace () {

    TanukiMojoSupport.deleteTree(workspace);
  }

  private Path stageDistribution (String operatingSystem)
    throws Exception {

    Path buildDirectory = Files.createDirectories(workspace.resolve("target"));
    Path outputDirectory = Files.createDirectories(buildDirectory.resolve("classes"));
    Path buildArtifactFile = Files.write(workspace.resolve("sample-app.jar"), "primary artifact".getBytes(StandardCharsets.UTF_8));
    Path dependencyFile = Files.write(workspace.resolve("library-1.0.jar"), "a runtime dependency".getBytes(StandardCharsets.UTF_8));

    MavenProject project = new MavenProject();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId(APP_NAME);
    project.setVersion(VERSION);

    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    build.setOutputDirectory(outputDirectory.toString());
    project.setBuild(build);

    Artifact buildArtifact = new DefaultArtifact("org.smallmind.test", APP_NAME, VERSION, "compile", "jar", null, new DefaultArtifactHandler("jar"));

    buildArtifact.setFile(buildArtifactFile.toFile());
    project.setArtifact(buildArtifact);

    DefaultArtifactHandler runtimeHandler = new DefaultArtifactHandler("jar");

    runtimeHandler.setAddedToClasspath(true);

    DefaultArtifact dependencyArtifact = new DefaultArtifact("org.smallmind.test", "library", "1.0", "runtime", "jar", null, runtimeHandler);

    dependencyArtifact.setFile(dependencyFile.toFile());
    project.setArtifacts(Set.<Artifact>of(dependencyArtifact));

    GenerateWrapperMojo mojo = new GenerateWrapperMojo();

    TanukiMojoSupport.setField(mojo, "project", project);
    TanukiMojoSupport.setField(mojo, "operatingSystem", operatingSystem);
    TanukiMojoSupport.setField(mojo, "wrapperListener", "org.smallmind.test.SampleWrapperListener");
    TanukiMojoSupport.setField(mojo, "applicationDir", "application");
    TanukiMojoSupport.setField(mojo, "applicationName", APP_NAME);
    TanukiMojoSupport.setField(mojo, "applicationLongName", "Sample Application");
    TanukiMojoSupport.setField(mojo, "umask", "0022");
    TanukiMojoSupport.setField(mojo, "javaCommand", "java");
    TanukiMojoSupport.setField(mojo, "compression", "zip");
    TanukiMojoSupport.setField(mojo, "createArtifact", true);
    TanukiMojoSupport.setField(mojo, "includeVersion", true);
    TanukiMojoSupport.setField(mojo, "compactClasspath", true);
    TanukiMojoSupport.setField(mojo, "useUpstart", false);
    TanukiMojoSupport.setField(mojo, "useSystemD", false);
    TanukiMojoSupport.setField(mojo, "waitAfterStartup", 0);
    TanukiMojoSupport.setField(mojo, "skip", false);
    TanukiMojoSupport.setField(mojo, "verbose", false);

    mojo.execute();

    return buildDirectory.resolve("application").resolve(APP_NAME + "-" + VERSION);
  }

  public void testUnixDistributionIsStagedWithRenderedScriptsAndBinaries ()
    throws Exception {

    Path applicationRoot = stageDistribution("LINUX_X86_64");

    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve(APP_NAME + ".sh")), "the rendered launch script is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve("wrapper-linux-x86-64")), "the wrapper executable is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("libwrapper-linux-x86-64.so")), "the platform wrapper library is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("libwrapper.so")), "the generic wrapper library is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("library-1.0.jar")), "the runtime dependency was not staged");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("conf").resolve("wrapper.conf")), "the rendered wrapper.conf is missing");
    Assert.assertTrue(Files.isRegularFile(workspace.resolve("target").resolve(APP_NAME + "-" + VERSION + "-app.zip")), "the aggregate archive is missing");

    String wrapperConf = Files.readString(applicationRoot.resolve("conf").resolve("wrapper.conf"), StandardCharsets.UTF_8);

    Assert.assertTrue(wrapperConf.contains("org.smallmind.test.SampleWrapperListener"), "the wrapper listener was not interpolated into wrapper.conf");
  }

  // A build that turns on the optional settings: a license and extra configuration files, JVM memory/args, a run-as
  // account, application parameters, environment and service entries, an additional (provided-scope) dependency, an
  // explicit (non-compacted) classpath, an unversioned application directory, a non-jar packaging that forces the
  // project output to be repackaged, and a suppressed aggregate archive.
  public void testFullyConfiguredUnixDistributionExercisesOptionalSettings ()
    throws Exception {

    Path buildDirectory = Files.createDirectories(workspace.resolve("target"));
    Path outputDirectory = Files.createDirectories(buildDirectory.resolve("classes"));
    Path licenseFile = Files.write(workspace.resolve("LICENSE.txt"), "license text".getBytes(StandardCharsets.UTF_8));
    Path configurationFile = Files.write(workspace.resolve("custom.conf"), "custom=value".getBytes(StandardCharsets.UTF_8));
    Path runtimeDependency = Files.write(workspace.resolve("runtime-1.0.jar"), "runtime".getBytes(StandardCharsets.UTF_8));
    Path providedDependency = Files.write(workspace.resolve("provided-2.0.jar"), "provided".getBytes(StandardCharsets.UTF_8));

    Files.write(outputDirectory.resolve("marker.txt"), "class output".getBytes(StandardCharsets.UTF_8));

    MavenProject project = new MavenProject();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId(APP_NAME);
    project.setVersion(VERSION);

    Build build = new Build();

    build.setDirectory(buildDirectory.toString());
    build.setOutputDirectory(outputDirectory.toString());
    project.setBuild(build);

    // A war-typed primary artifact forces the "repackage the output directory" branch rather than copying a jar.
    project.setArtifact(new DefaultArtifact("org.smallmind.test", APP_NAME, VERSION, "compile", "war", null, new DefaultArtifactHandler("war")));

    DefaultArtifactHandler classpathHandler = new DefaultArtifactHandler("jar");

    classpathHandler.setAddedToClasspath(true);

    DefaultArtifact runtimeArtifact = new DefaultArtifact("org.smallmind.test", "runtime", "1.0", "runtime", "jar", null, classpathHandler);
    DefaultArtifact providedArtifact = new DefaultArtifact("org.smallmind.test", "provided", "2.0", "provided", "jar", null, classpathHandler);

    runtimeArtifact.setFile(runtimeDependency.toFile());
    providedArtifact.setFile(providedDependency.toFile());
    project.setArtifacts(new java.util.LinkedHashSet<>(java.util.List.of(runtimeArtifact, providedArtifact)));

    EnvironmentArgument environmentArgument = new EnvironmentArgument();

    environmentArgument.setName("APP_HOME");
    environmentArgument.setValue("/opt/sample-app");

    Dependency additionalDependency = new Dependency();

    additionalDependency.setGroupId("org.smallmind.test");
    additionalDependency.setArtifactId("provided");

    GenerateWrapperMojo mojo = new GenerateWrapperMojo();

    TanukiMojoSupport.setField(mojo, "project", project);
    TanukiMojoSupport.setField(mojo, "operatingSystem", "LINUX_X86_64");
    TanukiMojoSupport.setField(mojo, "wrapperListener", "org.smallmind.test.SampleWrapperListener");
    TanukiMojoSupport.setField(mojo, "applicationDir", "application");
    TanukiMojoSupport.setField(mojo, "applicationName", APP_NAME);
    TanukiMojoSupport.setField(mojo, "applicationLongName", "Sample Application");
    TanukiMojoSupport.setField(mojo, "applicationDescription", "a fully configured sample");
    TanukiMojoSupport.setField(mojo, "licenseFile", licenseFile.toString());
    TanukiMojoSupport.setField(mojo, "configurations", new String[] {configurationFile.toString()});
    TanukiMojoSupport.setField(mojo, "toSourceFiles", new String[] {"setenv.sh", "extra.sh"});
    TanukiMojoSupport.setField(mojo, "jvmArgs", new String[] {"-Xss512k"});
    TanukiMojoSupport.setField(mojo, "jvmInitMemoryMB", "64");
    TanukiMojoSupport.setField(mojo, "jvmMaxMemoryMB", "256");
    TanukiMojoSupport.setField(mojo, "runAs", "appuser");
    TanukiMojoSupport.setField(mojo, "appParameters", new String[] {"--profile", "production"});
    TanukiMojoSupport.setField(mojo, "envArgs", new EnvironmentArgument[] {environmentArgument});
    TanukiMojoSupport.setField(mojo, "serviceDependencies", new String[] {"network"});
    TanukiMojoSupport.setField(mojo, "dependencies", new Dependency[] {additionalDependency});
    TanukiMojoSupport.setField(mojo, "umask", "0022");
    TanukiMojoSupport.setField(mojo, "javaCommand", "java");
    TanukiMojoSupport.setField(mojo, "compression", "zip");
    TanukiMojoSupport.setField(mojo, "createArtifact", false);
    TanukiMojoSupport.setField(mojo, "includeVersion", false);
    TanukiMojoSupport.setField(mojo, "compactClasspath", false);
    TanukiMojoSupport.setField(mojo, "useUpstart", false);
    TanukiMojoSupport.setField(mojo, "useSystemD", false);
    TanukiMojoSupport.setField(mojo, "waitAfterStartup", 0);
    TanukiMojoSupport.setField(mojo, "skip", false);
    TanukiMojoSupport.setField(mojo, "verbose", true);

    mojo.execute();

    // includeVersion=false drops the version from the application directory name.
    Path applicationRoot = buildDirectory.resolve("application").resolve(APP_NAME);

    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("conf").resolve("LICENSE.txt")), "the license file was not copied");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("conf").resolve("custom.conf")), "the extra configuration was not copied");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("runtime-1.0.jar")), "the runtime dependency was not staged");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("provided-2.0.jar")), "the additional provided dependency was not staged");
    // A non-jar packaging repackages the build output directory into a jar named for the project.
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve(APP_NAME + "-" + VERSION + ".jar")), "the repackaged project output is missing");
    Assert.assertFalse(Files.exists(buildDirectory.resolve(APP_NAME + "-" + VERSION + "-app.zip")), "the aggregate archive should be suppressed when createArtifact is false");

    String wrapperConf = Files.readString(applicationRoot.resolve("conf").resolve("wrapper.conf"), StandardCharsets.UTF_8);

    Assert.assertTrue(wrapperConf.contains("wrapper.java.initmemory=64"), "init memory was not rendered");
    Assert.assertTrue(wrapperConf.contains("wrapper.java.maxmemory=256"), "max memory was not rendered");
    Assert.assertTrue(wrapperConf.contains("set.APP_HOME=/opt/sample-app"), "the environment argument was not rendered");
    Assert.assertTrue(wrapperConf.contains("appuser"), "the run-as account was not rendered");
    Assert.assertTrue(wrapperConf.contains("runtime-1.0.jar"), "the explicit classpath entry was not rendered");
  }

  public void testWindowsDistributionIsStagedWithBatchFilesAndBinaries ()
    throws Exception {

    Path applicationRoot = stageDistribution("WINDOWS_X86_64");

    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve(APP_NAME + ".bat")), "the command batch file is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve("Install" + APP_NAME + "-NT.bat")), "the install batch file is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve("Uninstall" + APP_NAME + "-NT.bat")), "the uninstall batch file is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("bin").resolve("wrapper-windows-x86-64.exe")), "the wrapper executable is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("wrapper-windows-x86-64.dll")), "the platform wrapper library is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("lib").resolve("wrapper.dll")), "the generic wrapper library is missing");
    Assert.assertTrue(Files.isRegularFile(applicationRoot.resolve("conf").resolve("wrapper.conf")), "the rendered wrapper.conf is missing");
  }
}

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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.smallmind.spark.singularity.boot.SingularityEntryPoint;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * End-to-end coverage of the {@code generate-singularity} goal in modular mode. Real modules are compiled at test time
 * (the test sources themselves run on the class path and cannot carry descriptors), bundled by the Mojo, and launched
 * in a child JVM. The application module must load from the module layer the entry point defines and observe module
 * semantics: a service bound through {@code uses}/{@code provides}, a multi-release jar whose descriptor exists only
 * under {@code META-INF/versions/9}, an automatic module that reads a class path jar, resources visible through an
 * unconditional {@code opens} and hidden otherwise, and an unexported package that stays inaccessible.
 */
@org.testng.annotations.Test(groups = "integration")
public class SingularityModularBundleIntegrationTest {

  private static final String MAIN_CLASS = "test.app.main.Main";
  private static final String EXPECTED_REPORT = "test.app|greeted-by-module@test.greeter|versioned@test.versioned|test.auto:unnamed|open=true|closed=false|internal=encapsulated";

  private Path workspace;
  private Path bundle;
  private Path bootJar;
  private List<Artifact> dependencyArtifactList;

  private static void compile (Path outputDirectory, Map<String, String> sourceMap, String... options)
    throws Exception {

    JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
    ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    Path sourceDirectory = Files.createTempDirectory(outputDirectory.getParent(), "src");
    List<String> argumentList = new ArrayList<>();

    Files.createDirectories(outputDirectory);
    argumentList.add("-d");
    argumentList.add(outputDirectory.toString());
    argumentList.addAll(List.of(options));

    for (Map.Entry<String, String> sourceEntry : sourceMap.entrySet()) {

      Path sourceFile = sourceDirectory.resolve(sourceEntry.getKey());

      Files.createDirectories(sourceFile.getParent());
      Files.writeString(sourceFile, sourceEntry.getValue(), StandardCharsets.UTF_8);
      argumentList.add(sourceFile.toString());
    }

    if (javaCompiler.run(null, null, errorStream, argumentList.toArray(new String[0])) != 0) {
      throw new IllegalStateException("fixture compilation failed:\n" + errorStream.toString(StandardCharsets.UTF_8));
    }
  }

  private static Map<String, byte[]> readTree (Path root, String prefix)
    throws Exception {

    LinkedHashMap<String, byte[]> entryMap = new LinkedHashMap<>();

    try (Stream<Path> walk = Files.walk(root)) {
      for (Path file : walk.filter(Files::isRegularFile).toList()) {
        entryMap.put(prefix + root.relativize(file).toString().replace(File.separatorChar, '/'), Files.readAllBytes(file));
      }
    }

    return entryMap;
  }

  private static void writeJar (Path jarPath, Map<String, String> manifestAttributeMap, Map<String, byte[]> entryMap)
    throws Exception {

    Manifest manifest = new Manifest();

    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    for (Map.Entry<String, String> attributeEntry : manifestAttributeMap.entrySet()) {
      manifest.getMainAttributes().put(new Attributes.Name(attributeEntry.getKey()), attributeEntry.getValue());
    }

    try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
      for (Map.Entry<String, byte[]> entry : entryMap.entrySet()) {
        jarOutputStream.putNextEntry(new JarEntry(entry.getKey()));
        jarOutputStream.write(entry.getValue());
        jarOutputStream.closeEntry();
      }
    }
  }

  private static Artifact runtimeArtifact (String artifactId, Path jarPath) {

    DefaultArtifactHandler runtimeHandler = new DefaultArtifactHandler("jar");
    DefaultArtifact artifact;

    runtimeHandler.setAddedToClasspath(true);
    artifact = new DefaultArtifact("org.smallmind.test", artifactId, "1.0.0", "runtime", "jar", null, runtimeHandler);
    artifact.setFile(jarPath.toFile());

    return artifact;
  }

  @BeforeClass
  public void buildBundle ()
    throws Exception {

    workspace = Files.createTempDirectory("singularity-modular-it");

    Path fixtureDirectory = Files.createDirectories(workspace.resolve("fixtures"));
    Path classPathJar = workspace.resolve("test-cp.jar");
    Path automaticJar = workspace.resolve("test-auto.jar");
    Path greeterJar = workspace.resolve("test-greeter.jar");
    Path versionedJar = workspace.resolve("test-versioned.jar");
    Path buildDirectory = Files.createDirectories(workspace.resolve("build"));
    Path classesDirectory = buildDirectory.resolve("classes");
    LinkedHashMap<String, byte[]> greeterEntryMap;
    LinkedHashMap<String, byte[]> versionedEntryMap;

    compile(fixtureDirectory.resolve("cp"), Map.of("test/cp/CpThing.java", """
      package test.cp;
      
      public class CpThing {
      
        public static String where () {
      
          return CpThing.class.getModule().isNamed() ? "named" : "unnamed";
        }
      }
      """));
    writeJar(classPathJar, Map.of(), readTree(fixtureDirectory.resolve("cp"), ""));

    compile(fixtureDirectory.resolve("auto"), Map.of("test/auto/AutoHelper.java", """
      package test.auto;
      
      import test.cp.CpThing;
      
      public class AutoHelper {
      
        public static String describe () {
      
          return AutoHelper.class.getModule().getName() + ":" + CpThing.where();
        }
      }
      """), "-cp", fixtureDirectory.resolve("cp").toString());
    writeJar(automaticJar, Map.of("Automatic-Module-Name", "test.auto"), readTree(fixtureDirectory.resolve("auto"), ""));

    compile(fixtureDirectory.resolve("greeter"), Map.of("module-info.java", """
      module test.greeter {
      
        exports test.greeter;
      
        opens test.greeter.data;
      
        provides test.greeter.Greeting with test.greeter.internal.DefaultGreeting;
      }
      """, "test/greeter/Greeting.java", """
      package test.greeter;
      
      public interface Greeting {
      
        String greet ();
      }
      """, "test/greeter/internal/DefaultGreeting.java", """
      package test.greeter.internal;
      
      import test.greeter.Greeting;
      
      public class DefaultGreeting implements Greeting {
      
        public String greet () {
      
          return "greeted-by-module@" + DefaultGreeting.class.getModule().getName();
        }
      }
      """, "test/greeter/data/Marker.java", """
      package test.greeter.data;
      
      public class Marker {
      
      }
      """));
    greeterEntryMap = new LinkedHashMap<>(readTree(fixtureDirectory.resolve("greeter"), ""));
    greeterEntryMap.put("test/greeter/data/open.txt", "open".getBytes(StandardCharsets.UTF_8));
    greeterEntryMap.put("test/greeter/internal/closed.txt", "closed".getBytes(StandardCharsets.UTF_8));
    writeJar(greeterJar, Map.of(), greeterEntryMap);

    compile(fixtureDirectory.resolve("versioned-base"), Map.of("test/versioned/Version.java", """
      package test.versioned;
      
      public class Version {
      
        public static String get () {
      
          return "base";
        }
      }
      """));
    compile(fixtureDirectory.resolve("versioned-11"), Map.of("module-info.java", """
      module test.versioned {
      
        exports test.versioned;
      }
      """, "test/versioned/Version.java", """
      package test.versioned;
      
      public class Version {
      
        public static String get () {
      
          return "versioned@" + Version.class.getModule().getName();
        }
      }
      """));
    versionedEntryMap = new LinkedHashMap<>(readTree(fixtureDirectory.resolve("versioned-base"), ""));
    versionedEntryMap.put("META-INF/versions/9/module-info.class", Files.readAllBytes(fixtureDirectory.resolve("versioned-11").resolve("module-info.class")));
    versionedEntryMap.put("META-INF/versions/11/test/versioned/Version.class", Files.readAllBytes(fixtureDirectory.resolve("versioned-11").resolve("test/versioned/Version.class")));
    writeJar(versionedJar, Map.of("Multi-Release", "true"), versionedEntryMap);

    compile(classesDirectory, Map.of("module-info.java", """
      module test.app {
      
        requires test.auto;
        requires test.greeter;
        requires test.versioned;
      
        uses test.greeter.Greeting;
      }
      """, "test/app/main/Main.java", """
      package test.app.main;
      
      import java.nio.charset.StandardCharsets;
      import java.nio.file.Files;
      import java.nio.file.Path;
      import java.util.ServiceLoader;
      import test.auto.AutoHelper;
      import test.greeter.Greeting;
      import test.versioned.Version;
      
      public class Main {
      
        public static void main (String[] args)
          throws Exception {
      
          ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
          String internal;
      
          try {
            Class.forName("test.greeter.internal.DefaultGreeting", true, contextClassLoader).getConstructor().newInstance();
            internal = "accessible";
          } catch (IllegalAccessException illegalAccessException) {
            internal = "encapsulated";
          }
      
          Files.writeString(Path.of(args[0]), Main.class.getModule().getName()
            + "|" + ServiceLoader.load(Greeting.class).findFirst().orElseThrow().greet()
            + "|" + Version.get()
            + "|" + AutoHelper.describe()
            + "|open=" + (contextClassLoader.getResource("test/greeter/data/open.txt") != null)
            + "|closed=" + (contextClassLoader.getResource("test/greeter/internal/closed.txt") != null)
            + "|internal=" + internal, StandardCharsets.UTF_8);
        }
      }
      """), "--module-path", greeterJar + File.pathSeparator + versionedJar + File.pathSeparator + automaticJar);

    bootJar = MojoTestSupport.bootClassesAsJar(Path.of(SingularityEntryPoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()), workspace.resolve("spark-singularity-boot.jar"));
    dependencyArtifactList = List.of(runtimeArtifact("test-cp", classPathJar), runtimeArtifact("test-auto", automaticJar), runtimeArtifact("test-greeter", greeterJar), runtimeArtifact("test-versioned", versionedJar));

    executeMojo(buildDirectory, true);

    bundle = buildDirectory.resolve("sample-app-1.0.0.jar");
  }

  private MavenProject executeMojo (Path buildDirectory, boolean modular)
    throws Exception {

    MavenProject project = new MavenProject();
    Build build = new Build();
    DefaultArtifact bootArtifact = new DefaultArtifact("org.smallmind", "spark-singularity-boot", "7.4.0-SNAPSHOT", "compile", "jar", null, new DefaultArtifactHandler("jar"));
    GenerateSingularityMojo mojo = new GenerateSingularityMojo();

    project.setGroupId("org.smallmind.test");
    project.setArtifactId("sample-app");
    project.setVersion("1.0.0");
    build.setDirectory(buildDirectory.toString());
    project.setBuild(build);
    project.setArtifact(new DefaultArtifact("org.smallmind.test", "sample-app", "1.0.0", "compile", "jar", null, new DefaultArtifactHandler("jar")));
    project.setArtifacts(Set.copyOf(dependencyArtifactList));
    bootArtifact.setFile(bootJar.toFile());

    MojoTestSupport.setField(mojo, "project", project);
    MojoTestSupport.setField(mojo, "singularityBuildDir", "singularity");
    MojoTestSupport.setField(mojo, "mainClass", MAIN_CLASS);
    MojoTestSupport.setField(mojo, "skip", false);
    MojoTestSupport.setField(mojo, "verbose", false);
    MojoTestSupport.setField(mojo, "modular", modular);
    MojoTestSupport.setField(mojo, "exclusions", null);
    MojoTestSupport.setField(mojo, "pluginArtifacts", List.of(bootArtifact));
    MojoTestSupport.setField(mojo, "artifactFactory", new StubArtifactFactory());

    mojo.execute();

    return project;
  }

  @AfterClass(alwaysRun = true)
  public void deleteWorkspace () {

    MojoTestSupport.deleteTree(workspace);
  }

  public void testModularBundleBootsIntoTheModuleLayer ()
    throws Exception {

    Path marker = workspace.resolve("marker.txt");
    List<String> command = new ArrayList<>();

    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-jar");
    command.add(bundle.toString());
    command.add(marker.toString());

    ProcessBuilder processBuilder = new ProcessBuilder(command);

    processBuilder.redirectErrorStream(true);

    Process process = processBuilder.start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

    if (!process.waitFor(60, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      Assert.fail("the bundle's child JVM did not exit within the timeout");
    }

    Assert.assertEquals(process.exitValue(), 0, "the bundle's child JVM exited abnormally; output:\n" + output);
    Assert.assertEquals(Files.readString(marker, StandardCharsets.UTF_8), EXPECTED_REPORT);
  }

  public void testSplitPackageFailsTheBuild ()
    throws Exception {

    Path fixtureDirectory = workspace.resolve("fixtures");
    Path shadowJar = workspace.resolve("test-shadow.jar");
    Path buildDirectory = Files.createDirectories(workspace.resolve("split-build"));
    List<Artifact> originalArtifactList = dependencyArtifactList;

    // test.shadow is bound into the graph only as a Greeting provider, and its package collides with test.greeter's
    compile(fixtureDirectory.resolve("shadow"), Map.of("module-info.java", """
      module test.shadow {
      
        requires test.greeter;
      
        provides test.greeter.Greeting with test.greeter.internal.ShadowGreeting;
      }
      """, "test/greeter/internal/ShadowGreeting.java", """
      package test.greeter.internal;
      
      import test.greeter.Greeting;
      
      public class ShadowGreeting implements Greeting {
      
        public String greet () {
      
          return "shadow";
        }
      }
      """), "--module-path", workspace.resolve("test-greeter.jar").toString());
    writeJar(shadowJar, Map.of(), readTree(fixtureDirectory.resolve("shadow"), ""));
    Files.walkFileTree(workspace.resolve("build").resolve("classes"), new CopyFileVisitor(null, buildDirectory.resolve("classes")));

    dependencyArtifactList = new ArrayList<>(originalArtifactList);
    dependencyArtifactList.add(runtimeArtifact("test-shadow", shadowJar));

    try {
      executeMojo(buildDirectory, true);
      Assert.fail("modular mode accepted a module path with a split package");
    } catch (MojoExecutionException mojoExecutionException) {
      Assert.assertTrue(mojoExecutionException.getMessage().contains("test.greeter.internal"), mojoExecutionException.getMessage());
    } finally {
      dependencyArtifactList = originalArtifactList;
    }
  }

  public void testModularModeRequiresAModuleDescriptor ()
    throws Exception {

    Path buildDirectory = Files.createDirectories(workspace.resolve("plain-build"));

    MojoTestSupport.writeClassInto(buildDirectory.resolve("classes"), "org.smallmind.spark.singularity.app.EntryPointApp");

    try {
      executeMojo(buildDirectory, true);
      Assert.fail("modular mode accepted a project without a module descriptor");
    } catch (MojoExecutionException mojoExecutionException) {
      Assert.assertTrue(mojoExecutionException.getMessage().contains("named module"), mojoExecutionException.getMessage());
    }
  }
}

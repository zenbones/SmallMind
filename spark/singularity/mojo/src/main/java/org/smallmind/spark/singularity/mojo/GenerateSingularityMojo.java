/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.spark.singularity.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.maven.CompressionType;
import org.smallmind.spark.singularity.boot.SingularityEntryPoint;
import org.smallmind.spark.singularity.boot.SingularityIndex;

// Generates Singularity based one jar applications
@Mojo(name = "generate-singularity", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateSingularityMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "plugin.artifacts")
  protected List<Artifact> pluginArtifacts;
  @Component
  ArtifactFactory artifactFactory;
  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private Exclusion[] exclusions;
  @Parameter(defaultValue = "singularity")
  private String singularityBuildDir;
  @Parameter
  private String mainClass;
  @Parameter(defaultValue = "false")
  private boolean verbose;
  @Parameter(defaultValue = "false")
  private boolean skip;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    if (!skip) {

      Artifact applicationArtifact;
      SingularityIndex singularityIndex = new SingularityIndex();
      Path buildPath;
      Path libraryPath;
      Path indexPath;
      Path classesPath;
      File compressedFile;
      boolean bootClassesFound = false;

      try {
        Files.createDirectories(buildPath = Paths.get(project.getBuild().getDirectory(), singularityBuildDir));
        Files.createDirectories(libraryPath = buildPath.resolve("META-INF").resolve("singularity"));
        Files.createDirectories(indexPath = buildPath.resolve("META-INF").resolve("index"));
      } catch (IOException ioException) {
        throw new MojoExecutionException("Unable to create a build directory", ioException);
      }

      for (Artifact pluginArtifact : pluginArtifacts) {
        if (pluginArtifact.getGroupId().equals("org.smallmind") && pluginArtifact.getArtifactId().equals("spark-singularity-boot")) {
          try {
            copyBootClasses(singularityIndex, pluginArtifact.getFile(), buildPath);
          } catch (IOException ioException) {
            throw new MojoExecutionException("Problem in copying boot classes into the build directory", ioException);
          }

          bootClassesFound = true;
          break;
        }
      }
      if (!bootClassesFound) {
        throw new MojoExecutionException("Unable to locate the boot class dependencies");
      }

      for (Artifact artifact : project.getRuntimeArtifacts()) {

        boolean excluded = false;

        if ((exclusions != null) && (exclusions.length > 0)) {
          for (Exclusion exclusion : exclusions) {
            if (exclusion.matchesArtifact(artifact)) {
              excluded = true;
              break;
            }
          }
        }

        if (excluded) {
          if (verbose) {
            getLog().info(String.format("Excluded dependency(%s)...", artifact.getFile().getName()));
          }
        } else {
          if (verbose) {
            getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
          }

          try {

            JarFile jarFile = new JarFile(artifact.getFile());
            Enumeration<JarEntry> jarEntryEnum = jarFile.entries();

            while (jarEntryEnum.hasMoreElements()) {
              singularityIndex.addInverseJarEntry(jarEntryEnum.nextElement().getName(), artifact.getFile().getName());
            }

            copyToDestination(artifact.getFile(), libraryPath.resolve(artifact.getFile().getName()));
          } catch (IOException ioException) {
            throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the build directory", artifact), ioException);
          }
        }
      }

      if (Files.exists(classesPath = Paths.get(project.getBuild().getDirectory(), "classes"))) {
        if (verbose) {
          getLog().info("Copying classes directory...");
        }
        try {
          Files.walkFileTree(classesPath, new CopyFileVisitor(singularityIndex, buildPath));
        } catch (IOException ioException) {
          throw new MojoExecutionException("Unable to copy the classes directory into the build path", ioException);
        }
      }

      if (verbose) {
        getLog().info("Creating singularity index...");
      }
      try {
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(indexPath.resolve("singularity.idx").toFile()))) {
          objectOutputStream.writeObject(singularityIndex);
        }
      } catch (IOException ioException) {
        throw new MojoExecutionException("Unable to write the singularity index", ioException);
      }

      try {

        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();

        attributes.put(Attributes.Name.MAIN_CLASS, SingularityEntryPoint.class.getName());
        attributes.put(new Attributes.Name("Singularity-Class"), mainClass);

        attributes.put(Attributes.Name.SPECIFICATION_TITLE, System.getProperty("java.vm.specification.name"));
        attributes.put(Attributes.Name.SPECIFICATION_VERSION, System.getProperty("java.vm.specification.version"));
        attributes.put(Attributes.Name.SPECIFICATION_VENDOR, System.getProperty("java.vm.specification.vendor"));
        attributes.put(Attributes.Name.IMPLEMENTATION_TITLE, System.getProperty("java.specification.name"));
        attributes.put(Attributes.Name.IMPLEMENTATION_VERSION, System.getProperty("java.specification.version"));
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, System.getProperty("java.specification.vendor"));

        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");

        if (verbose) {
          getLog().info("Compressing output jar...");
        }

        CompressionType.JAR.compress(compressedFile = Paths.get(project.getBuild().getDirectory(), constructArtifactName()).toFile(), buildPath.toFile(), manifest);
      } catch (IOException ioException) {
        throw new MojoExecutionException("Problem constructing the executable jar", ioException);
      }

      applicationArtifact = artifactFactory.createArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(), "compile", "jar");
      applicationArtifact.setFile(compressedFile);

      project.addAttachedArtifact(applicationArtifact);
    }
  }

  private String constructArtifactName () {

    StringBuilder nameBuilder = new StringBuilder(project.getArtifactId()).append('-').append(project.getVersion());

    if (project.getArtifact().getClassifier() != null) {
      nameBuilder.append('-').append(project.getArtifact().getClassifier());
    }

    return nameBuilder.append(".jar").toString();
  }

  private void copyToDestination (File file, Path destinationPath)
    throws IOException {

    FileInputStream inputStream;
    FileOutputStream outputStream;
    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = (inputStream = new FileInputStream(file)).getChannel();
    writeChannel = (outputStream = new FileOutputStream(destinationPath.toFile())).getChannel();
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    outputStream.close();
    inputStream.close();
  }

  private void copyBootClasses (SingularityIndex singularityIndex, File jarFile, Path destinationPath)
    throws IOException {

    byte[] buffer = new byte[1024];

    try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {

      JarEntry jarEntry;

      while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
        if ((!jarEntry.isDirectory()) && jarEntry.getName().startsWith("org/smallmind/spark/singularity/boot/")) {
          if (verbose) {
            getLog().info(String.format("Copying boot class(%s)...", jarEntry.getName()));
          }

          Files.createDirectories(destinationPath.resolve(jarEntry.getName().substring(0, jarEntry.getName().lastIndexOf('/'))));

          try (OutputStream outputStream = Files.newOutputStream(destinationPath.resolve(jarEntry.getName()), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

            long totalBytesToRead = jarEntry.getSize();
            int totalBytesRead = 0;
            int bytesRead;

            do {
              bytesRead = jarInputStream.read(buffer, 0, (int)Math.min(buffer.length, totalBytesToRead - totalBytesRead));
              outputStream.write(buffer, 0, bytesRead);
              totalBytesRead += bytesRead;
            } while (totalBytesRead < totalBytesToRead);
          }

          singularityIndex.addFileName(jarEntry.getName());
        }
      }
    }
  }
}

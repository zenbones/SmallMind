/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.freemarker.ClassPathTemplateLoader;
import org.smallmind.nutsnbolts.io.PathUtility;
import org.smallmind.nutsnbolts.zip.CompressionType;

// Generates Tanukisoft based os service wrappers
@Mojo(name = "generate-wrapper", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateWrapperMojo extends AbstractMojo {

  private static final EnvironmentArgument[] NO_ENV_ARGS = new EnvironmentArgument[0];
  private static final String[] NO_SIMPLE_ARGS = new String[0];
  private static final String RESOURCE_BASE_PATH = GenerateWrapperMojo.class.getPackage().getName().replace('.', '/');

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private String licenseFile;
  @Parameter
  private Dependency[] dependencies;
  @Parameter(required = true)
  private String operatingSystem;
  @Parameter(required = true)
  private String wrapperListener;
  @Parameter(defaultValue = "application")
  private String applicationDir;
  @Parameter(property = "project.artifactId")
  private String applicationName;
  @Parameter(property = "project.name")
  private String applicationLongName;
  @Parameter
  private String classifier;
  @Parameter(property = "project.description")
  private String applicationDescription;
  @Parameter
  private String[] jvmArgs;
  @Parameter
  private String jvmInitMemoryMB;
  @Parameter
  private String jvmMaxMemoryMB;
  @Parameter(defaultValue = "0022")
  private String umask;
  @Parameter
  private String runAs;
  @Parameter(defaultValue = "0")
  private int waitAfterStartup;
  @Parameter(defaultValue = "java")
  private String javaCommand;
  @Parameter
  private String[] appParameters;
  @Parameter
  private EnvironmentArgument[] envArgs;
  @Parameter
  private String[] serviceDependencies;
  @Parameter
  private String[] configurations;
  @Parameter
  private String[] toSourceFiles;
  @Parameter(defaultValue = "false")
  private boolean useUpstart;
  @Parameter(defaultValue = "false")
  private boolean useSystemD;
  @Parameter(defaultValue = "zip")
  private String compression;
  @Parameter(defaultValue = "true")
  private boolean createArtifact;
  @Parameter(defaultValue = "true")
  private boolean includeVersion;
  @Parameter(defaultValue = "true")
  private boolean compactClasspath;
  @Parameter(defaultValue = "false")
  private boolean verbose;
  @Parameter(defaultValue = "false")
  private boolean skip;

  public void execute ()
    throws MojoExecutionException {

    if (!skip) {

      Path binDirectory;
      Path libDirectory;
      Path confDirectory;
      OSType osType;
      CompressionType compressionType;
      HashMap<String, Object> freemarkerMap;
      LinkedList<String> classpathElementList;
      List<Dependency> additionalDependencies;
      Iterator<Dependency> additionalDependencyIter;
      StringBuilder aggregateFilesToSource;

      try {
        osType = OSType.valueOf(operatingSystem.replace('-', '_').toUpperCase());
        if (verbose) {
          getLog().info(String.format("Using os type(%s)...", osType.name()));
        }
      } catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown operating system type(%s) - valid choices are %s", operatingSystem, Arrays.toString(OSType.values())), exception);
      }

      try {
        compressionType = CompressionType.valueOf(compression.replace('-', '_').toUpperCase());
        if (verbose) {
          getLog().info(String.format("Using compression type(%s)...", compressionType.name()));
        }
      } catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown compression type(%s) - valid choices are %s", compression, Arrays.toString(CompressionType.values())), exception);
      }

      createDirectory("bin", binDirectory = Paths.get(project.getBuild().getDirectory(), applicationDir, constructArtifactName(includeVersion, false), "bin"));
      createDirectory("lib", libDirectory = Paths.get(project.getBuild().getDirectory(), applicationDir, constructArtifactName(includeVersion, false), "lib"));
      createDirectory("conf", confDirectory = Paths.get(project.getBuild().getDirectory(), applicationDir, constructArtifactName(includeVersion, false), "conf"));

      if (licenseFile != null) {
        try {

          Path licensePath;

          if (verbose) {
            getLog().info(String.format("Copying license file(%s)...", licenseFile));
          }

          copyToDestination(licensePath = Paths.get(licenseFile), confDirectory, PathUtility.fileNameAsString(licensePath.getFileName()));
        } catch (IOException ioException) {
          throw new MojoExecutionException("Problem in copying your license file into the application conf directory", ioException);
        }
      }

      if (configurations != null) {
        for (String configuration : configurations) {

          Path configurationPath;

          try {
            if (verbose) {
              getLog().info(String.format("Copying configuration(%s)...", configuration));
            }

            copyToDestination(configurationPath = Paths.get(configuration), confDirectory, PathUtility.fileNameAsString(configurationPath));
          } catch (IOException ioException) {
            throw new MojoExecutionException(String.format("Problem in copying the configuration(%s) into the application conf directory", configuration), ioException);
          }
        }
      }

      aggregateFilesToSource = new StringBuilder();
      if (toSourceFiles != null) {
        for (String toSourceFile : toSourceFiles) {
          if (aggregateFilesToSource.length() > 0) {
            aggregateFilesToSource.append(';');
          }

          aggregateFilesToSource.append(toSourceFile);
        }
      }

      freemarkerMap = new HashMap<>();
      freemarkerMap.put("applicationName", applicationName);
      freemarkerMap.put("applicationLongName", applicationLongName);
      freemarkerMap.put("applicationDescription", (applicationDescription != null) ? applicationDescription : String.format("%s generated project", GenerateWrapperMojo.class.getSimpleName()));
      freemarkerMap.put("javaCommand", javaCommand);
      freemarkerMap.put("wrapperListener", wrapperListener);
      freemarkerMap.put("jvmArgs", (jvmArgs != null) ? jvmArgs : NO_SIMPLE_ARGS);
      freemarkerMap.put("envArgs", (envArgs != null) ? envArgs : NO_ENV_ARGS);

      if ((jvmInitMemoryMB != null) && (!jvmInitMemoryMB.isEmpty())) {
        freemarkerMap.put("jvmInitMemoryMB", jvmInitMemoryMB);
      }
      if ((jvmMaxMemoryMB != null) && (!jvmMaxMemoryMB.isEmpty())) {
        freemarkerMap.put("jvmMaxMemoryMB", jvmMaxMemoryMB);
      }
      if ((runAs != null) && (!runAs.isEmpty())) {
        freemarkerMap.put("runAs", runAs);
      }

      freemarkerMap.put("umask", umask);

      freemarkerMap.put("useUpstart", useUpstart);
      freemarkerMap.put("useSystemD", useSystemD);

      freemarkerMap.put("waitAfterStartup", String.valueOf(waitAfterStartup));

      if (appParameters == null) {
        freemarkerMap.put("appParameters", new String[] {wrapperListener});
      } else {

        String[] modifiedAppParameters = new String[appParameters.length + 1];

        modifiedAppParameters[0] = wrapperListener;
        System.arraycopy(appParameters, 0, modifiedAppParameters, 1, appParameters.length);
        freemarkerMap.put("appParameters", modifiedAppParameters);
      }

      freemarkerMap.put("serviceDependencies", (serviceDependencies != null) ? serviceDependencies : NO_SIMPLE_ARGS);

      classpathElementList = new LinkedList<>();
      freemarkerMap.put("classpathElements", classpathElementList);
      if (compactClasspath) {
        classpathElementList.add("*");
      }

      freemarkerMap.put("filesToSource", aggregateFilesToSource.toString());

      additionalDependencies = (dependencies != null) ? Arrays.asList(dependencies) : null;
      for (Artifact artifact : project.getArtifacts()) {
        if (artifact.getArtifactHandler().isAddedToClasspath() && (Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_RUNTIME.equals(artifact.getScope()))) {
          try {
            if (verbose) {
              getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
            }

            if (additionalDependencies != null) {
              additionalDependencyIter = additionalDependencies.iterator();
              while (additionalDependencyIter.hasNext()) {
                if (additionalDependencyIter.next().matchesArtifact(artifact)) {
                  additionalDependencyIter.remove();
                }
              }
            }

            if (!compactClasspath) {
              classpathElementList.add(artifact.getFile().getName());
            }
            copyToDestination(artifact.getFile(), libDirectory, artifact.getFile().getName());
          } catch (IOException ioException) {
            throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the application library", artifact), ioException);
          }
        }
      }

      if (additionalDependencies != null) {
        for (Dependency dependency : additionalDependencies) {
          for (Artifact artifact : project.getArtifacts()) {
            if ((Artifact.SCOPE_COMPILE.equals(artifact.getScope()) || Artifact.SCOPE_PROVIDED.equals(artifact.getScope()) || Artifact.SCOPE_SYSTEM.equals(artifact.getScope())) && dependency.matchesArtifact(artifact)) {
              try {
                if (verbose) {
                  getLog().info(String.format("Copying additional dependency(%s)...", artifact.getFile().getName()));
                }

                if (!compactClasspath) {
                  classpathElementList.add(artifact.getFile().getName());
                }
                copyToDestination(artifact.getFile(), libDirectory, artifact.getFile().getName());
              } catch (IOException ioException) {
                throw new MojoExecutionException(String.format("Problem in copying an additional dependency(%s) into the application library", artifact), ioException);
              }
            }
          }
        }
      }

      if (!project.getArtifact().getType().equals("jar")) {

        Path jarPath = constructCompressedArtifactPath(project.getBuild().getDirectory(), CompressionType.JAR, false);
        String jarFileName = PathUtility.fileNameAsString(jarPath);

        try {
          if (verbose) {
            getLog().info(String.format("Creating and copying output jar(%s)...", jarFileName));
          }

          CompressionType.JAR.compress(Paths.get(project.getBuild().getOutputDirectory()), jarPath);

          if (!compactClasspath) {
            classpathElementList.add(jarFileName);
          }
          copyToDestination(jarPath, libDirectory, jarFileName);
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in creating or copying the output jar(%s) into the application library", jarFileName), ioException);
        }
      } else {
        try {
          if (verbose) {
            getLog().info(String.format("Copying build artifact(%s)...", project.getArtifact().getFile().getName()));
          }

          if (!compactClasspath) {
            classpathElementList.add(project.getArtifact().getFile().getName());
          }
          copyToDestination(project.getArtifact().getFile(), libDirectory, project.getArtifact().getFile().getName());
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in copying the build artifact(%s) into the application library", project.getArtifact()), ioException);
        }
      }

      try {
        if (verbose) {
          getLog().info(String.format("Copying wrapper library(%s)...", osType.getLibrary()));
        }

        copyToDestination(getResourceAsStream(getWrapperPath("lib", osType.getLibrary())), libDirectory, osType.getLibrary());
        copyToDestination(getResourceAsStream(getWrapperPath("lib", osType.getLibrary())), libDirectory, osType.getOsStyle().getLibrary());
      } catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the wrapper library(%s) into the application library", osType.getLibrary()), ioException);
      }

      try {
        if (verbose) {
          getLog().info(String.format("Copying wrapper executable(%s)...", osType.getExecutable()));
        }

        copyToDestination(getResourceAsStream(getWrapperPath("bin", osType.getExecutable())), binDirectory, osType.getExecutable());
      } catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the wrapper executable(%s) into the application binaries", osType.getExecutable()), ioException);
      }

      try {
        if (verbose) {
          getLog().info("Copying wrapper scripts...");
        }

        switch (osType.getOsStyle()) {
          case UNIX:
            processFreemarkerTemplate(getWrapperPath("bin", "freemarker.App.sh.in"), binDirectory, applicationName + ".sh", freemarkerMap);
            break;
          case WINDOWS:
            copyToDestination(getResourceAsStream(getWrapperPath("bin", "AppCommand.bat.in")), binDirectory, applicationName + ".bat");
            copyToDestination(getResourceAsStream(getWrapperPath("bin", "InstallApp-NT.bat.in")), binDirectory, "Install" + applicationName + "-NT.bat");
            copyToDestination(getResourceAsStream(getWrapperPath("bin", "UninstallApp-NT.bat.in")), binDirectory, "Uninstall" + applicationName + "-NT.bat");
            break;
          default:
            throw new MojoExecutionException(String.format("Unknown os style(%s)", osType.getOsStyle().name()));
        }
      } catch (IOException ioException) {
        throw new MojoExecutionException("Problem in copying the wrapper scripts into the application binaries", ioException);
      }

      if (verbose) {
        getLog().info("Processing the configuration template...");
      }

      processFreemarkerTemplate(getWrapperPath("conf", "freemarker.wrapper.conf.in"), confDirectory, "wrapper.conf", freemarkerMap);

      if (createArtifact) {

        Path compressedFile;

        compressedFile = constructCompressedArtifactPath(project.getBuild().getDirectory(), compressionType, true);

        try {
          if (verbose) {
            getLog().info(String.format("Creating aggregated %s(%s)...", compressionType.getExtension(), compressedFile.getFileName()));
          }

          compressionType.compress(Paths.get(project.getBuild().getDirectory(), applicationDir), compressedFile);
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in creating the aggregated %s(%s)", compressionType.getExtension(), compressedFile.getFileName()), ioException);
        }
      }
    }
  }

  private String constructArtifactName (boolean includeVersion, boolean aggregateArtifact) {

    StringBuilder nameBuilder;

    nameBuilder = new StringBuilder(applicationName);

    if (includeVersion) {
      nameBuilder.append('-').append(project.getVersion());
    }

    if ((classifier != null) && (!classifier.isEmpty())) {
      nameBuilder.append('-').append(classifier);
    }

    if (aggregateArtifact) {
      nameBuilder.append("-app");
    }

    return nameBuilder.toString();
  }

  private Path constructCompressedArtifactPath (String outputDir, CompressionType artifactCompressionType, boolean aggregateArtifact) {

    return Paths.get(outputDir, constructArtifactName(true, aggregateArtifact) + '.' + artifactCompressionType.getExtension());
  }

  private void processFreemarkerTemplate (Path templatePath, Path outputPath, String destinationFileName, HashMap<String, Object> interpolationMap)
    throws MojoExecutionException {

    Configuration freemarkerConf;
    Template freemarkerTemplate;
    Writer freemarkerWriter;

    freemarkerConf = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(GenerateWrapperMojo.class));

    try {
      freemarkerTemplate = freemarkerConf.getTemplate(PathUtility.asResourceString(templatePath));
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Unable to load template(%s) for translation", destinationFileName), ioException);
    }

    try {
      freemarkerWriter = Files.newBufferedWriter(outputPath.resolve(destinationFileName));
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in creating a writer for the template(%s) file", destinationFileName), ioException);
    }

    try {
      freemarkerTemplate.process(interpolationMap, freemarkerWriter);
    } catch (Exception exception) {
      throw new MojoExecutionException(String.format("Problem in processing the template(%s)", destinationFileName), exception);
    }

    try {
      freemarkerWriter.close();
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in closing the template(%s) writer", destinationFileName), ioException);
    }
  }

  private void createDirectory (String dirType, Path dirPath)
    throws MojoExecutionException {

    try {
      Files.createDirectories(dirPath);
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Unable to create the '%s' application directory(%s)", dirType, dirPath.toAbsolutePath()));
    }
  }

  private Path getWrapperPath (String dirType, String fileName) {

    return Paths.get(RESOURCE_BASE_PATH, dirType, fileName);
  }

  private InputStream getResourceAsStream (Path path)
    throws MojoExecutionException {

    InputStream inputStream;

    if ((inputStream = GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(PathUtility.asResourceString(path))) == null) {
      throw new MojoExecutionException(String.format("Unable to find resource at the specified path(%s)", path));
    }

    return inputStream;
  }

  private void copyToDestination (Path sourcePath, Path destinationPath, String destinationFileName)
    throws IOException {

    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = FileChannel.open(sourcePath, StandardOpenOption.READ);
    writeChannel = FileChannel.open(destinationPath.resolve(destinationFileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    writeChannel.close();
    readChannel.close();
  }

  private void copyToDestination (File file, Path destinationPath, String destinationFileName)
    throws IOException {

    try (FileInputStream inputStream = new FileInputStream(file)) {
      copyToDestination(inputStream, destinationPath, destinationFileName);
    }
  }

  private void copyToDestination (InputStream inputStream, Path destinationPath, String destinationFileName)
    throws IOException {

    OutputStream outputStream;
    byte[] buffer = new byte[8192];
    int bytesRead;

    outputStream = Files.newOutputStream(destinationPath.resolve(destinationFileName), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();
    inputStream.close();
  }
}

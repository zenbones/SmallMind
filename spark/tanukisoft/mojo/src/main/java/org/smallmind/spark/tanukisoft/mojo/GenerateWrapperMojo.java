/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.spark.tanukisoft.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
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
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.freemarker.ClassPathTemplateLoader;

// Generates Tanukisoft based os service wrappers
@Mojo(name = "generate-wrapper", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateWrapperMojo extends AbstractMojo {

  private static final String[] NO_ARGS = new String[0];
  private static final String RESOURCE_BASE_PATH = GenerateWrapperMojo.class.getPackage().getName().replace('.', '/');

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private File licenseFile;
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
  @Parameter(property = "project.description")
  private String applicationDescription;
  @Parameter
  private String[] jvmArgs;
  @Parameter(defaultValue = "0")
  private int jvmInitMemoryMB;
  @Parameter(defaultValue = "0")
  private int jvmMaxMemoryMB;
  @Parameter
  private String runAs;
  @Parameter
  private String withPassword;
  @Parameter
  private String umask;
  @Parameter(defaultValue = "0")
  private int waitAfterStartup;
  @Parameter(defaultValue = "java")
  private String javaCommand;
  @Parameter
  private String[] appParameters;
  @Parameter
  private String[] serviceDependencies;
  @Parameter
  private String[] configurations;
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
  @Parameter(defaultValue = "true")
  private boolean engaged;

  public void execute ()
   throws MojoExecutionException, MojoFailureException {

    if (engaged) {

      File binDirectory;
      File libDirectory;
      File confDirectory;
      OSType osType;
      CompressionType compressionType;
      HashMap<String, Object> freemarkerMap;
      LinkedList<String> classpathElementList;
      List<Dependency> additionalDependencies;
      Iterator<Dependency> aditionalDependencyIter;

      try {
        osType = OSType.valueOf(operatingSystem.replace('-', '_').toUpperCase());
      } catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown operating system type(%s) - valid choices are %s", operatingSystem, Arrays.toString(OSType.values())), exception);
      }

      try {
        compressionType = CompressionType.valueOf(compression.replace('-', '_').toUpperCase());
      } catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown compression type(%s) - valid choices are %s", compression, Arrays.toString(CompressionType.values())), exception);
      }

      createDirectory("bin", binDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + constructArtifactName(includeVersion, false) + System.getProperty("file.separator") + "bin"));
      createDirectory("lib", libDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + constructArtifactName(includeVersion, false) + System.getProperty("file.separator") + "lib"));
      createDirectory("conf", confDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir + System.getProperty("file.separator") + constructArtifactName(includeVersion, false) + System.getProperty("file.separator") + "conf"));

      if (licenseFile != null) {
        try {
          if (verbose) {
            getLog().info(String.format("Copying license file(%s)...", licenseFile.getAbsolutePath()));
          }

          copyToDestination(new FileInputStream(licenseFile), confDirectory.getAbsolutePath(), licenseFile.getName());
        } catch (IOException ioException) {
          throw new MojoExecutionException("Problem in copying your license file into the application conf directory", ioException);
        }
      }

      if (configurations != null) {
        for (String configuration : configurations) {

          File configurationFile = new File(configuration);

          try {
            if (verbose) {
              getLog().info(String.format("Copying configuration(%s)...", configurationFile.getName()));
            }

            copyToDestination(configurationFile, confDirectory.getAbsolutePath(), configurationFile.getName());
          } catch (IOException ioException) {
            throw new MojoExecutionException(String.format("Problem in copying the configuration(%s) into the application conf directory", configurationFile.getAbsolutePath()), ioException);
          }
        }
      }

      freemarkerMap = new HashMap<String, Object>();
      freemarkerMap.put("applicationName", applicationName);
      freemarkerMap.put("applicationLongName", applicationLongName);
      freemarkerMap.put("applicationDescription", (applicationDescription != null) ? applicationDescription : String.format("%s generated project", GenerateWrapperMojo.class.getSimpleName()));
      freemarkerMap.put("javaCommand", javaCommand);
      freemarkerMap.put("wrapperListener", wrapperListener);
      freemarkerMap.put("jvmArgs", (jvmArgs != null) ? jvmArgs : NO_ARGS);

      if (jvmInitMemoryMB > 0) {
        freemarkerMap.put("jvmInitMemoryMB", jvmInitMemoryMB);
      }
      if (jvmMaxMemoryMB > 0) {
        freemarkerMap.put("jvmMaxMemoryMB", jvmMaxMemoryMB);
      }
      if ((runAs != null) && (runAs.length() > 0)) {
        freemarkerMap.put("runAs", runAs);
      }
      if ((withPassword != null) && (withPassword.length()) > 0) {
        freemarkerMap.put("withPassword", withPassword);
      }
      if ((umask != null) && (umask.length() > 0)) {
        freemarkerMap.put("umask", umask);
      }

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

      freemarkerMap.put("serviceDependencies", (serviceDependencies != null) ? serviceDependencies : NO_ARGS);

      classpathElementList = new LinkedList<String>();
      freemarkerMap.put("classpathElements", classpathElementList);
      if (compactClasspath) {
        classpathElementList.add("*");
      }

      additionalDependencies = (dependencies != null) ? Arrays.asList(dependencies) : null;
      for (Artifact artifact : project.getRuntimeArtifacts()) {
        try {
          if (verbose) {
            getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
          }

          if (additionalDependencies != null) {
            aditionalDependencyIter = additionalDependencies.iterator();
            while (aditionalDependencyIter.hasNext()) {
              if (aditionalDependencyIter.next().matchesArtifact(artifact)) {
                aditionalDependencyIter.remove();
              }
            }
          }

          if (!compactClasspath) {
            classpathElementList.add(artifact.getFile().getName());
          }
          copyToDestination(artifact.getFile(), libDirectory.getAbsolutePath(), artifact.getFile().getName());
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the application library", artifact), ioException);
        }
      }

      if (additionalDependencies != null) {
        for (Dependency dependency : additionalDependencies) {
          for (Artifact artifact : project.getDependencyArtifacts()) {
            if (dependency.matchesArtifact(artifact)) {
              try {
                if (verbose) {
                  getLog().info(String.format("Copying additional dependency(%s)...", artifact.getFile().getName()));
                }

                if (!compactClasspath) {
                  classpathElementList.add(artifact.getFile().getName());
                }
                copyToDestination(artifact.getFile(), libDirectory.getAbsolutePath(), artifact.getFile().getName());
              } catch (IOException ioException) {
                throw new MojoExecutionException(String.format("Problem in copying an additional dependency(%s) into the application library", artifact), ioException);
              }
            }
          }
        }
      }

      if (!project.getArtifact().getType().equals("jar")) {

        File jarFile;

        jarFile = new File(constructCompressedArtifactPath(project.getBuild().getDirectory(), CompressionType.JAR, false));

        try {
          if (verbose) {
            getLog().info(String.format("Creating and copying output jar(%s)...", jarFile.getName()));
          }

          CompressionType.JAR.compress(jarFile, new File(project.getBuild().getOutputDirectory()));

          if (!compactClasspath) {
            classpathElementList.add(jarFile.getName());
          }
          copyToDestination(jarFile, libDirectory.getAbsolutePath(), jarFile.getName());
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in creating or copying the output jar(%s) into the application library", jarFile.getName()), ioException);
        }
      } else {
        try {
          if (verbose) {
            getLog().info(String.format("Copying build artifact(%s)...", project.getArtifact().getFile().getName()));
          }

          if (!compactClasspath) {
            classpathElementList.add(project.getArtifact().getFile().getName());
          }
          copyToDestination(project.getArtifact().getFile(), libDirectory.getAbsolutePath(), project.getArtifact().getFile().getName());
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in copying the build artifact(%s) into the application library", project.getArtifact()), ioException);
        }
      }

      try {
        if (verbose) {
          getLog().info(String.format("Copying wrapper library(%s)...", osType.getLibrary()));
        }

        copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("lib", osType.getLibrary())), libDirectory.getAbsolutePath(), osType.getLibrary());
        copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("lib", osType.getLibrary())), libDirectory.getAbsolutePath(), osType.getOsStyle().getLibrary());
      } catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the wrapper library(%s) into the application library", osType.getLibrary()), ioException);
      }

      try {
        if (verbose) {
          getLog().info(String.format("Copying wrapper executable(%s)...", osType.getExecutable()));
        }

        copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", osType.getExecutable())), binDirectory.getAbsolutePath(), osType.getExecutable());
      } catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the wrapper executable(%s) into the application binaries", osType.getExecutable()), ioException);
      }

      try {
        if (verbose) {
          getLog().info("Copying wrapper scripts...");
        }

        switch (osType.getOsStyle()) {
          case UNIX:
            processFreemarkerTemplate(getWrapperFilePath("bin", "freemarker.sh.script.in"), binDirectory, applicationName + ".sh", freemarkerMap);
            break;
          case WINDOWS:
            copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "App.bat.in")), binDirectory.getAbsolutePath(), applicationName + ".bat");
            copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "InstallApp-NT.bat.in")), binDirectory.getAbsolutePath(), "Install" + applicationName + "-NT.bat");
            copyToDestination(GenerateWrapperMojo.class.getClassLoader().getResourceAsStream(getWrapperFilePath("bin", "UninstallApp-NT.bat.in")), binDirectory.getAbsolutePath(), "Uninstall" + applicationName + "-NT.bat");
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

      processFreemarkerTemplate(getWrapperFilePath("conf", "freemarker.wrapper.conf.in"), confDirectory, "wrapper.conf", freemarkerMap);

      if (createArtifact) {

        File compressedFile;

        compressedFile = new File(constructCompressedArtifactPath(project.getBuild().getDirectory(), compressionType, true));

        try {
          if (verbose) {
            getLog().info(String.format("Creating aggregated %s(%s)...", compressionType.getExtension(), compressedFile.getName()));
          }

          compressionType.compress(compressedFile, new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir));
        } catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in creating the aggregated %s(%s)", compressionType.getExtension(), compressedFile.getName()), ioException);
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

    if (project.getArtifact().getClassifier() != null) {
      nameBuilder.append('-').append(project.getArtifact().getClassifier());
    }

    if (aggregateArtifact) {
      nameBuilder.append("-app");
    }

    return nameBuilder.toString();
  }

  private String constructCompressedArtifactPath (String outputPath, CompressionType artifactCompressionType, boolean aggregateArtifact) {

    return new StringBuilder(outputPath).append(System.getProperty("file.separator")).append(constructArtifactName(true, aggregateArtifact)).append('.').append(artifactCompressionType.getExtension()).toString();
  }

  private void processFreemarkerTemplate (String templatePath, File outputDir, String destinationName, HashMap<String, Object> interpolationMap)
   throws MojoExecutionException {

    Configuration freemarkerConf;
    Template freemarkerTemplate;
    FileWriter fileWriter;

    freemarkerConf = new Configuration();
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(GenerateWrapperMojo.class));

    try {
      freemarkerTemplate = freemarkerConf.getTemplate(templatePath);
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Unable to load template(%s) for translation", destinationName), ioException);
    }

    try {
      fileWriter = new FileWriter(outputDir.getAbsolutePath() + System.getProperty("file.separator") + destinationName);
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in creating a writer for the template(%s) file", destinationName), ioException);
    }

    try {
      freemarkerTemplate.process(interpolationMap, fileWriter);
    } catch (Exception exception) {
      throw new MojoExecutionException(String.format("Problem in processing the template(%s)", destinationName), exception);
    }

    try {
      fileWriter.close();
    } catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in closing the template(%s) writer", destinationName), ioException);
    }
  }

  private void createDirectory (String dirType, File dirFile)
   throws MojoExecutionException {

    if (!dirFile.isDirectory()) {
      if (!dirFile.mkdirs()) {
        throw new MojoExecutionException(String.format("Unable to create the '%s' application directory(%s)", dirType, dirFile.getAbsolutePath()));
      }
    }
  }

  private String getWrapperFilePath (String dirType, String fileName) {

    StringBuilder pathBuilder;

    pathBuilder = new StringBuilder(RESOURCE_BASE_PATH);
    pathBuilder.append('/');
    pathBuilder.append(dirType);
    pathBuilder.append('/');
    pathBuilder.append(fileName);

    return pathBuilder.toString();
  }

  public void copyToDestination (File file, String destinationPath, String destinationName)
   throws IOException {

    FileInputStream inputStream;
    FileOutputStream outputStream;
    FileChannel readChannel;
    FileChannel writeChannel;
    long bytesTransferred;
    long currentPosition = 0;

    readChannel = (inputStream = new FileInputStream(file)).getChannel();
    writeChannel = (outputStream = new FileOutputStream(destinationPath + System.getProperty("file.separator") + destinationName)).getChannel();
    while ((currentPosition < readChannel.size()) && (bytesTransferred = readChannel.transferTo(currentPosition, 8192, writeChannel)) >= 0) {
      currentPosition += bytesTransferred;
    }
    outputStream.close();
    inputStream.close();
  }

  public void copyToDestination (InputStream inputStream, String destinationPath, String destinationName)
   throws IOException {

    FileOutputStream outputStream;
    byte[] buffer = new byte[8192];
    int bytesRead;

    outputStream = new FileOutputStream(destinationPath + System.getProperty("file.separator") + destinationName);
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      outputStream.write(buffer, 0, bytesRead);
    }
    outputStream.close();
    inputStream.close();
  }
}
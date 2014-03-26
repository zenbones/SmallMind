/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
package org.smallmind.javafx.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
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
import org.smallmind.nutsnbolts.io.FileIterator;
import org.smallmind.nutsnbolts.util.SingleItemIterator;

// Generates A Webstart Javafx-based Project
@Mojo(name = "generate-jnlp", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class GenerateJNLPMojo extends AbstractMojo {

  private static final JNLParameter[] NO_PARAMETERS = new JNLParameter[0];
  private static final String[] NO_ARGS = new String[0];
  private static final String RESOURCE_BASE_PATH = GenerateJNLPMojo.class.getPackage().getName().replace('.', '/');

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(required = true)
  private String operatingSystem;
  @Parameter(required = true)
  private String javafxRuntime;
  @Parameter(required = true)
  private String javaVersion;
  @Parameter(defaultValue = "-Xms64m -Xmx256m")
  private String jvmArgs;
  @Parameter(defaultValue = "1.0")
  private String jnlpSpec;
  @Parameter(required = true)
  private String mainClass;
  @Parameter(defaultValue = "800")
  private int width;
  @Parameter(defaultValue = "600")
  private int height;
  @Parameter(required = true)
  private Dependency javafx;
  @Parameter
  private SigningInfo signjar;
  @Parameter
  private JNLParameter[] jnlpParameters;
  @Parameter
  private String[] jnlpArguments;
  @Parameter(property = "project.artifactId")
  private String applicationName;
  @Parameter(property = "project.artifactId")
  private String title;
  @Parameter(property = "roject.groupId")
  private String vendor;
  @Parameter(property = "project.artifactId")
  private String description;
  @Parameter(defaultValue = "true")
  private boolean offlineAllowed;
  @Parameter(defaultValue = "jnlp")
  private String deployDir;
  @Parameter(defaultValue = "true")
  private boolean createJar;
  @Parameter(defaultValue = "true")
  private boolean includeVersion;
  @Parameter(defaultValue = "true")
  private boolean includeHref;
  @Parameter(defaultValue = "false")
  private boolean verbose;

  @Override
  public void execute ()
    throws MojoExecutionException {

    File deployDirectory;
    HashMap<String, Object> freemarkerMap;
    LinkedList<JNLPDependency> dependencyList;
    OSType osType;
    JavaFXRuntimeVersion runtimeVersion;
    J2SEVersion j2seVersion;
    String runtimeLocation;
    boolean javafxFound = false;

    try {
      osType = OSType.valueOf(operatingSystem.replace('-', '_').toUpperCase());
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("Unknown operating system type(%s) - valid choices are %s", operatingSystem, Arrays.toString(OSType.values())), exception);
    }

    try {
      runtimeVersion = JavaFXRuntimeVersion.fromCode(javafxRuntime);
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("Unknown javafx runtime type(%s) - valid choices are %s", javafxRuntime, Arrays.toString(JavaFXRuntimeVersion.getValidCodes())), exception);
    }

    try {
      runtimeLocation = runtimeVersion.getLocation(osType);
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("The javafx runtime (%s) is not available on os type (%s)", runtimeVersion.getCode(), osType.name()), exception);
    }

    try {
      j2seVersion = J2SEVersion.fromCode(javaVersion);
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("Unknown java version(%s) - valid choices are %s", javaVersion, Arrays.toString(J2SEVersion.getValidCodes())), exception);
    }

    freemarkerMap = new HashMap<String, Object>();
    freemarkerMap.put("jnlpSpec", jnlpSpec);
    freemarkerMap.put("includeHref", includeHref);
    freemarkerMap.put("href", createArtifactName(includeVersion, false));
    freemarkerMap.put("title", title);
    freemarkerMap.put("vendor", vendor);
    freemarkerMap.put("description", description);
    freemarkerMap.put("offlineAllowed", offlineAllowed);
    freemarkerMap.put("runtimeVersion", runtimeVersion.getCode());
    freemarkerMap.put("runtimeLocation", runtimeLocation);
    freemarkerMap.put("j2seVersion", j2seVersion.getCode());
    freemarkerMap.put("jvmArgs", jvmArgs);
    freemarkerMap.put("j2seLocation", j2seVersion.getLocation());
    freemarkerMap.put("mainClass", mainClass);
    freemarkerMap.put("width", width);
    freemarkerMap.put("height", height);
    freemarkerMap.put("jnlpParameters", (jnlpParameters != null) ? jnlpParameters : NO_PARAMETERS);
    freemarkerMap.put("jnlpArguments", (jnlpArguments != null) ? jnlpArguments : NO_ARGS);

    createDirectory("deploy", deployDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + deployDir + System.getProperty("file.separator") + createArtifactName(includeVersion, false) + System.getProperty("file.separator")));

    dependencyList = new LinkedList<>();
    copyDependencies(project.getRuntimeArtifacts(), deployDirectory, dependencyList);

    for (Artifact artifact : project.getDependencyArtifacts()) {
      if (javafx.matchesArtifact(artifact)) {
        copyDependencies(new SingleItemIterator<Artifact>(artifact), deployDirectory, dependencyList);
        javafxFound = true;
        break;
      }
    }
    if (!javafxFound) {
      throw new MojoExecutionException("Project does not reference the javafx dependency(group = %s, artifactId = %s)", javafx.getGroupId(), javafx.getArtifactId());
    }

    Collections.sort(dependencyList);

    if (!project.getArtifact().getType().equals("jar")) {

      File jarFile;

      jarFile = new File(createJarArtifactPath(project.getBuild().getDirectory(), false));

      try {

        long fileSize;

        if (verbose) {
          getLog().info(String.format("Creating and copying output jar(%s)...", jarFile.getName()));
        }

        createJar(jarFile, new File(project.getBuild().getOutputDirectory()));
        fileSize = copyToDestination(jarFile, deployDirectory.getAbsolutePath(), jarFile.getName());
        dependencyList.addFirst(new JNLPDependency(jarFile.getName(), fileSize));
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in creating or copying the output jar(%s) into the deployment directory", jarFile.getName()), ioException);
      }
    }
    else {
      try {

        long fileSize;

        if (verbose) {
          getLog().info(String.format("Copying build artifact(%s)...", project.getArtifact().getFile().getName()));
        }

        fileSize = copyToDestination(project.getArtifact().getFile(), deployDirectory.getAbsolutePath(), project.getArtifact().getFile().getName());
        dependencyList.addFirst(new JNLPDependency(project.getArtifact().getFile().getName(), fileSize));
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying the build artifact(%s) into the deployment directory", project.getArtifact()), ioException);
      }
    }

    if (signjar != null) {
      try {
        for (JNLPDependency dependency : dependencyList) {
          if (dependency.getName().endsWith(".jar")) {
            getLog().info(String.format("Signing jar(%s)...", dependency.getName()));

            if (signjar.isVerbose()) {
              sun.security.tools.JarSigner.main(new String[] {"-verbose", "-keystore", signjar.getKeystore(), "-storepass", signjar.getStorepass(), "-keypass", signjar.getKeypass(), "-sigfile", "SIGNATURE", deployDirectory.getAbsolutePath() + System.getProperty("file.separator") + dependency.getName(), signjar.getAlias()});
            }
            else {
              sun.security.tools.JarSigner.main(new String[] {"-keystore", signjar.getKeystore(), "-storepass", signjar.getStorepass(), "-keypass", signjar.getKeypass(), "-sigfile", "SIGNATURE", deployDirectory.getAbsolutePath() + System.getProperty("file.separator") + dependency.getName(), signjar.getAlias()});
            }
          }
        }
      }
      catch (Exception exception) {
        throw new MojoExecutionException("Unable to sign jar files...", exception);
      }
    }

    freemarkerMap.put("jnlpDependencies", dependencyList);

    if (verbose) {
      getLog().info("Processing the configuration template...");
    }

    processFreemarkerTemplate(getTemplateFilePath(), deployDirectory, createArtifactName(includeVersion, false) + ".jnlp", freemarkerMap);

    if (createJar) {

      File jarFile;

      jarFile = new File(createJarArtifactPath(project.getBuild().getDirectory(), true));

      try {
        if (verbose) {
          getLog().info(String.format("Creating aggregated jar(%s)...", jarFile.getName()));
        }

        createJar(jarFile, new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + deployDir));
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in creating the aggregated jar(%s)", jarFile.getName()), ioException);
      }
    }
  }

  private void createDirectory (String dirType, File dirFile)
    throws MojoExecutionException {

    if (!dirFile.isDirectory()) {
      if (!dirFile.mkdirs()) {
        throw new MojoExecutionException(String.format("Unable to create the '%s' webstart directory(%s)", dirType, dirFile.getAbsolutePath()));
      }
    }
  }

  public long copyToDestination (File file, String destinationPath, String destinationName)
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

    return currentPosition;
  }

  public void copyDependencies (Iterable<Artifact> artifactIterable, File deployDirectory, List<JNLPDependency> dependencyList)
    throws MojoExecutionException {

    for (Artifact artifact : artifactIterable) {

      boolean matched = false;

      for (JNLPDependency jnlpDependency : dependencyList) {
        if (jnlpDependency.getName().equals(artifact.getFile().getName())) {
          matched = true;
          break;
        }
      }

      if (!matched) {
        try {

          long fileSize;

          if (verbose) {
            getLog().info(String.format("Copying dependency(%s)...", artifact.getFile().getName()));
          }

          fileSize = copyToDestination(artifact.getFile(), deployDirectory.getAbsolutePath(), artifact.getFile().getName());
          dependencyList.add(new JNLPDependency(artifact.getFile().getName(), fileSize));
        }
        catch (IOException ioException) {
          throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the deployment directory", artifact), ioException);
        }
      }
    }
  }

  private String createArtifactName (boolean includeVersion, boolean aggregateArtifact) {

    StringBuilder nameBuilder;

    nameBuilder = new StringBuilder(applicationName);

    if (includeVersion) {
      nameBuilder.append('-').append(project.getVersion());
    }

    if (project.getArtifact().getClassifier() != null) {
      nameBuilder.append('-').append(project.getArtifact().getClassifier());
    }

    if (aggregateArtifact) {
      nameBuilder.append("-jnlp");
    }

    return nameBuilder.toString();
  }

  private String createJarArtifactPath (String outputPath, boolean aggregateArtifact) {

    return new StringBuilder(outputPath).append(System.getProperty("file.separator")).append(createArtifactName(true, aggregateArtifact)).append(".jar").toString();
  }

  private void createJar (File jarFile, File directoryToJar)
    throws IOException {

    FileOutputStream fileOutputStream;
    JarOutputStream jarOutputStream;
    JarEntry jarEntry;

    fileOutputStream = new FileOutputStream(jarFile);
    jarOutputStream = new JarOutputStream(fileOutputStream, new Manifest());
    for (File outputFile : new FileIterator(directoryToJar)) {
      if (!outputFile.equals(jarFile)) {
        jarEntry = new JarEntry(outputFile.getCanonicalPath().substring(directoryToJar.getAbsolutePath().length() + 1).replace(System.getProperty("file.separator"), "/"));
        jarEntry.setTime(outputFile.lastModified());
        jarOutputStream.putNextEntry(jarEntry);
        squeezeFile(jarOutputStream, outputFile);
      }
    }
    jarOutputStream.close();
    fileOutputStream.close();
  }

  private void squeezeFile (JarOutputStream jarOutputStream, File outputFile)
    throws IOException {

    FileInputStream inputStream;
    byte[] buffer = new byte[8192];
    int bytesRead;

    inputStream = new FileInputStream(outputFile);
    while ((bytesRead = inputStream.read(buffer)) >= 0) {
      jarOutputStream.write(buffer, 0, bytesRead);
    }
    inputStream.close();
  }

  private String getTemplateFilePath () {

    StringBuilder pathBuilder;

    pathBuilder = new StringBuilder(RESOURCE_BASE_PATH).append("/deploy/freemarker.jnlp.in");

    return pathBuilder.toString();
  }

  private void processFreemarkerTemplate (String templatePath, File outputDir, String destinationName, HashMap<String, Object> interpolationMap)
    throws MojoExecutionException {

    Configuration freemarkerConf;
    Template freemarkerTemplate;
    FileWriter fileWriter;

    freemarkerConf = new Configuration();
    freemarkerConf.setTagSyntax(freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    freemarkerConf.setTemplateLoader(new ClassPathTemplateLoader(GenerateJNLPMojo.class));

    try {
      freemarkerTemplate = freemarkerConf.getTemplate(templatePath);
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Unable to load template(%s) for translation", destinationName), ioException);
    }

    try {
      fileWriter = new FileWriter(outputDir.getAbsolutePath() + System.getProperty("file.separator") + destinationName);
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in creating a writer for the template(%s) file", destinationName), ioException);
    }

    try {
      freemarkerTemplate.process(interpolationMap, fileWriter);
    }
    catch (Exception exception) {
      throw new MojoExecutionException(String.format("Problem in processing the template(%s)", destinationName), exception);
    }

    try {
      fileWriter.close();
    }
    catch (IOException ioException) {
      throw new MojoExecutionException(String.format("Problem in closing the template(%s) writer", destinationName), ioException);
    }
  }
}

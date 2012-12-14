/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.webstart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * @goal generate-webstart
 * @phase package
 * @requiresDependencyResolution runtime
 * @description Generates A Webstart Javafx-based Deployment
 * @threadSafe
 */
public class GenerateWebstartMojo extends AbstractMojo {

  /**
   * @parameter expression="${project}"
   * @readonly
   */
  private MavenProject project;

  /**
   * @parameter
   * @required
   */
  private String operatingSystem;

  /**
   * @parameter
   * @required
   */
  private String javafxRuntime;

  /**
   * @parameter default-value="1.0"
   */
  private String jnlpSpec;

  /**
   * @parameter expression="${project.artifactId}"
   */
  private String applicationName;

  /**
   * @parameter expression="${project.artifactId}.jnlp"
   */
  private String href;

  /**
   * @parameter expression="${project.artifactId}"
   */
  private String title;

  /**
   * @parameter expression="${project.groupId}"
   */
  private String vendor;

  /**
   * @parameter expression="${project.artifactId}"
   */
  private String description;

  /**
   * @parameter default-value=true
   */
  private boolean offlineAllowed;

  /**
   * @parameter default-value="deploy"
   */
  private String deployDir;

  /**
   * @parameter default-value=true
   */
  private boolean createJar;

  /**
   * @parameter default-value=true
   */
  private boolean includeVersion;

  /**
   * @parameter default-value=false
   */
  private boolean verbose;

  @Override
  public void execute ()
    throws MojoExecutionException {

    File deployDirectory;
    HashMap<String, Object> freemarkerMap;
    OSType osType;
    RuntimeVersion runtimeVersion;
    String runtimeLocation;

    try {
      osType = OSType.valueOf(operatingSystem.replace('-', '_').toUpperCase());
    }
    catch (Throwable throwable) {
      throw new MojoExecutionException(String.format("Unknown operating system type(%s) - valid choices are %s", operatingSystem, Arrays.toString(OSType.values())), throwable);
    }

    try {
      runtimeVersion = RuntimeVersion.fromCode(javafxRuntime);
    }
    catch (Throwable throwable) {
      throw new MojoExecutionException(String.format("Unknown javafx runtime type(%s) - valid choices are %s", javafxRuntime, Arrays.toString(RuntimeVersion.getValidCodes())), throwable);
    }

    try {
      runtimeLocation = runtimeVersion.getLocation(osType);
    }
    catch (Throwable throwable) {
      throw new MojoExecutionException(String.format("The javafx runtime (%s) is not available on os type (%s)", runtimeVersion.getCode(), osType.name()), throwable);
    }

    freemarkerMap = new HashMap<String, Object>();
    freemarkerMap.put("jnlpSpec", jnlpSpec);
    freemarkerMap.put("href", href);
    freemarkerMap.put("title", title);
    freemarkerMap.put("vendor", vendor);
    freemarkerMap.put("description", description);
    freemarkerMap.put("offlineAllowed", offlineAllowed);
    freemarkerMap.put("runtimeVersion", runtimeVersion.getCode());
    freemarkerMap.put("runtimeLocation", runtimeLocation);

    createDirectory("deploy", deployDirectory = new File(project.getBuild().getDirectory() + System.getProperty("file.separator") + deployDir));

    for (Object artifact : project.getRuntimeArtifacts()) {
      try {
        if (verbose) {
          getLog().info(String.format("Copying dependency(%s)...", ((org.apache.maven.artifact.Artifact)artifact).getFile().getName()));
        }

        //TODO: do this
//        classpathElementList.add(((Artifact)artifact).getFile().getName());
        copyToDestination(((Artifact)artifact).getFile(), deployDirectory.getAbsolutePath(), ((Artifact)artifact).getFile().getName());
      }
      catch (IOException ioException) {
        throw new MojoExecutionException(String.format("Problem in copying a dependency(%s) into the application library", artifact), ioException);
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
}

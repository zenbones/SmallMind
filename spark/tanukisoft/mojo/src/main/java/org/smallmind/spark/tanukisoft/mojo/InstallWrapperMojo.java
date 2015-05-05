/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.spark.tanukisoft.mojo;

import java.io.File;
import java.util.Arrays;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

// Installs Tanukisoft based os service wrappers
@Mojo(name = "install-wrapper", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class InstallWrapperMojo extends AbstractMojo {

  @Component
  ArtifactFactory artifactFactory;
  @Component
  ArtifactInstaller artifactInstaller;
  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(readonly = true, property = "localRepository")
  private ArtifactRepository localRepository;
  @Parameter(property = "project.artifactId")
  private String applicationName;
  @Parameter(defaultValue = "zip")
  private String compression;
  @Parameter(defaultValue = "true")
  private boolean engaged;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    if (engaged) {

      Artifact applicationArtifact;
      CompressionType compressionType;
      StringBuilder pathBuilder;

      try {
        compressionType = CompressionType.valueOf(compression.replace('-', '_').toUpperCase());
      }
      catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown compression type(%s) - valid choices are %s", compression, Arrays.toString(CompressionType.values())), exception);
      }

      applicationArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), compressionType.getExtension(), (project.getArtifact().getClassifier() == null) ? "app" : project.getArtifact().getClassifier() + "-app");
      pathBuilder = new StringBuilder(project.getBuild().getDirectory()).append(System.getProperty("file.separator")).append(applicationName).append('-').append(project.getVersion());

      if (project.getArtifact().getClassifier() != null) {
        pathBuilder.append('-');
        pathBuilder.append(project.getArtifact().getClassifier());
      }

      pathBuilder.append("-app").append('.').append(compressionType.getExtension());

      try {
        artifactInstaller.install(new File(pathBuilder.toString()), applicationArtifact, localRepository);
      }
      catch (ArtifactInstallationException artifactInstallationException) {
        throw new MojoExecutionException("Unable to install the application(" + applicationName + ")", artifactInstallationException);
      }
    }
  }
}
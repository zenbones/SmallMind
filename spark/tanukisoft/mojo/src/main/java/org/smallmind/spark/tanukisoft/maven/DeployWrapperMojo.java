/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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

import java.nio.file.Paths;
import java.util.Arrays;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.smallmind.nutsnbolts.zip.CompressionType;

// Deploys Tanukisoft based os service wrappers
@Mojo(name = "deploy-wrapper", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DeployWrapperMojo extends AbstractMojo {

  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(readonly = true, property = "localRepository")
  private ArtifactRepository localRepository;
  @Parameter(defaultValue = "${project.distributionManagementArtifactRepository}")
  private ArtifactRepository deploymentRepository;
  @Parameter(property = "project.artifactId")
  private String applicationName;
  @Parameter(defaultValue = "zip")
  private String compression;
  @Parameter(defaultValue = "false")
  private boolean skip;
  @Component
  private ArtifactFactory artifactFactory;
  @Component
  private ArtifactDeployer artifactDeployer;

  public void execute ()
    throws MojoExecutionException {

    if (!skip) {

      Artifact applicationArtifact;
      CompressionType compressionType;
      StringBuilder nameBuilder;

      try {
        compressionType = CompressionType.valueOf(compression.replace('-', '_').toUpperCase());
      } catch (Exception exception) {
        throw new MojoExecutionException(String.format("Unknown compression type(%s) - valid choices are %s", compression, Arrays.toString(CompressionType.values())), exception);
      }

      applicationArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), compressionType.getExtension(), (project.getArtifact().getClassifier() == null) ? "app" : project.getArtifact().getClassifier() + "-app");
      nameBuilder = new StringBuilder(applicationName).append('-').append(project.getVersion());

      if (project.getArtifact().getClassifier() != null) {
        nameBuilder.append('-');
        nameBuilder.append(project.getArtifact().getClassifier());
      }

      nameBuilder.append("-app").append('.').append(compressionType.getExtension());

      try {
        artifactDeployer.deploy(Paths.get(project.getBuild().getDirectory(), nameBuilder.toString()).toFile(), applicationArtifact, deploymentRepository, localRepository);
      } catch (ArtifactDeploymentException artifactDeploymentException) {
        throw new MojoExecutionException("Unable to deploy the application(" + applicationName + ")", artifactDeploymentException);
      }
    }
  }
}

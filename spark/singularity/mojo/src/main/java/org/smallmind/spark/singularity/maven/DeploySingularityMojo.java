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
package org.smallmind.spark.singularity.maven;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

// Deploys Singularity based one jar applications
@Mojo(name = "deploy-singularity", defaultPhase = LifecyclePhase.INSTALL, threadSafe = true)
public class DeploySingularityMojo extends AbstractMojo {

  @Component
  ArtifactFactory artifactFactory;
  @Component
  ArtifactDeployer artifactDeployer;
  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter(readonly = true, property = "localRepository")
  private ArtifactRepository localRepository;
  @Parameter(defaultValue = "${project.distributionManagementArtifactRepository}")
  private ArtifactRepository deploymentRepository;
  @Parameter(defaultValue = "false")
  private boolean skip;

  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    if (!skip) {

      Artifact applicationArtifact;
      File artifactAscFile;
      StringBuilder pathBuilder;

      if (project.getArtifact().getClassifier() == null) {
        applicationArtifact = artifactFactory.createArtifact(project.getGroupId(), project.getArtifactId(), project.getVersion(), "compile", "jar");
      } else {
        applicationArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "jar", project.getArtifact().getClassifier());
      }

      pathBuilder = new StringBuilder(project.getBuild().getDirectory()).append(System.getProperty("file.separator")).append(project.getArtifactId()).append('-').append(project.getVersion());

      if (project.getArtifact().getClassifier() != null) {
        pathBuilder.append('-');
        pathBuilder.append(project.getArtifact().getClassifier());
      }

      pathBuilder.append(".jar");

      if ((artifactAscFile = new File(pathBuilder.toString() + ".asc")).isFile()) {
        applicationArtifact.addMetadata(new AscArtifactMetadata(applicationArtifact, artifactAscFile));
      }

      try {
        artifactDeployer.deploy(new File(pathBuilder.toString()), applicationArtifact, deploymentRepository, localRepository);
      } catch (ArtifactDeploymentException artifactDeploymentException) {
        throw new MojoExecutionException("Unable to deploy the singularity jar", artifactDeploymentException);
      }
    }
  }
}

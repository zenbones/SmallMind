/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.spark.tanukisoft.mojo;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal install-wrapper
 * @phase install
 * @description Installs Tanukisoft based os service wrappers
 * @threadSafe
 */
public class InstallWrapperMojo extends AbstractMojo {

   /**
    * @parameter expression="${project}"
    * @readonly
    */
   private MavenProject project;

   /**
    * @component
    * @readonly
    */
   ArtifactFactory artifactFactory;

   /**
    * @component
    * @readonly
    */
   ArtifactInstaller artifactInstaller;

   /**
    * @parameter expression="${localRepository}"
    * @readonly
    */
   private ArtifactRepository localRepository;

   /**
    * @parameter default-value="application"
    */
   private String applicationDir;

   /**
    * @parameter expression="${project.artifactId}"
    */
   private String applicationName;

   public void execute ()
      throws MojoExecutionException, MojoFailureException {

      Artifact applicationArtifact;
      StringBuilder nameBuilder;

      applicationArtifact = artifactFactory.createArtifactWithClassifier(project.getGroupId(), project.getArtifactId(), project.getVersion(), "jar", (project.getArtifact().getClassifier() == null) ? "app" : project.getArtifact().getClassifier() + "-app");

      nameBuilder = new StringBuilder(project.getBuild().getDirectory() + System.getProperty("file.separator") + applicationDir);

      nameBuilder.append(System.getProperty("file.separator"));
      nameBuilder.append(applicationName);
      nameBuilder.append('-');
      nameBuilder.append(project.getVersion());

      if (project.getArtifact().getClassifier() != null) {
         nameBuilder.append('-');
         nameBuilder.append(project.getArtifact().getClassifier());
      }

      nameBuilder.append("-app").append(".jar");

      try {
         artifactInstaller.install(new File(nameBuilder.toString()), applicationArtifact, localRepository);
      }
      catch (ArtifactInstallationException artifactInstallationException) {
         throw new MojoExecutionException("Unable to install the application(" + applicationName + ")", artifactInstallationException);
      }
   }
}
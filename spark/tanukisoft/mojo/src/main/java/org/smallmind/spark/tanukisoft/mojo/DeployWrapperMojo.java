package org.smallmind.spark.tanukisoft.mojo;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.deployer.ArtifactDeployer;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal deploy-wrapper
 * @phase deploy
 * @description Deploys Tanukisoft based os service wrappers
 */
public class DeployWrapperMojo extends AbstractMojo {

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
   ArtifactDeployer artifactDeployer;

   /**
    * @parameter expression="${localRepository}"
    * @readonly
    */
   private ArtifactRepository localRepository;

   /**
    * @parameter default-value="${project.distributionManagementArtifactRepository}"
    */
   private ArtifactRepository deploymentRepository;

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
         artifactDeployer.deploy(new File(nameBuilder.toString()), applicationArtifact, deploymentRepository, localRepository);
      }
      catch (ArtifactDeploymentException artifactDeploymentException) {
         throw new MojoExecutionException("Unable to deploy the application(" + applicationName + ")", artifactDeploymentException);
      }
   }
}
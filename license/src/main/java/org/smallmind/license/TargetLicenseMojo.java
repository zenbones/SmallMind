package org.smallmind.license;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.Stencil;

/**
 * @goal install-license-files
 * @phase process-resources
 * @description Installs license files for inclusion in distribution artifacts
 */
public class TargetLicenseMojo extends AbstractMojo {

   /**
    * @parameter expression="${project}"
    * @readonly
    */
   private MavenProject project;

   /**
    * @parameter
    */
   private Stencil[] stencils;

   /**
    * @parameter
    */
   private Rule[] rules;

   /**
    * @parameter default-value=false
    */
   private boolean verbose;

   @Override
   public void execute ()
      throws MojoExecutionException, MojoFailureException {

      MavenProject rootProject = project;

      while (!rootProject.isExecutionRoot()) {
         rootProject = rootProject.getParent();
      }

      for (Rule rule : rules) {
      }
   }

}

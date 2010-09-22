package org.smallmind.license;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.Stencil;

/**
 * @goal generate-license-headers
 * @phase verify
 * @aggregator
 * @description Generates and/or replaces license headers
 */
public class LicenseMojo extends AbstractMojo {

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

      
   }
}

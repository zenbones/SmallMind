package org.smallmind.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
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

      for (Rule rule : rules) {

         String[] licenseArray;
         File licenseFile;

         if (rule.getLicense() != null) {
            licenseFile = new File(rule.getLicense());
            licenseArray = getFileAsLineArray(licenseFile.isAbsolute() ? licenseFile.getAbsolutePath() : rootProject.getBasedir() + System.getProperty("file.separator") + licenseFile.getPath());

         }
      }
   }

   private String[] getFileAsLineArray (String licensePath)
      throws MojoExecutionException {

      BufferedReader licenseReader;
      LinkedList<String> lineList;
      String[] lineArray;
      String singleLine;

      try {
         licenseReader = new BufferedReader(new FileReader(licensePath));
         lineList = new LinkedList<String>();
         while ((singleLine = licenseReader.readLine()) != null) {
            lineList.add(singleLine);
         }
      }
      catch (IOException ioException) {
         throw new MojoExecutionException("Unable to acquire the license file(" + licensePath + ")", ioException);
      }

      lineArray = new String[lineList.size()];
      lineList.toArray(lineArray);

      return lineArray;
   }
}

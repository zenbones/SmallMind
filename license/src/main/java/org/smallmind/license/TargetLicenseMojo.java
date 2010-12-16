package org.smallmind.license;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * @goal install-license-files
 * @phase prepare-package
 * @description Installs license files for inclusion in distribution artifacts
 * @threadSafe
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
   private Rule[] rules;

   /**
    * @parameter
    */
   private String[] licenses;

   /**
    * @parameter default-value=false
    */
   private boolean verbose;

   @Override
   public void execute ()
      throws MojoExecutionException, MojoFailureException {

      if ((new File(project.getBuild().getOutputDirectory())).exists()) {

         MavenProject rootProject = project;
         byte[] buffer = new byte[8192];

         while (!(rootProject.getParent() == null)) {
            rootProject = rootProject.getParent();
         }

         for (String license : licenses) {

            File licenseFile;
            File copyFile;
            FileInputStream inputStream;
            FileOutputStream outputStream;
            int bytesRead;

            if (!(licenseFile = new File(license)).isAbsolute()) {
               licenseFile = new File(rootProject.getBasedir() + System.getProperty("file.separator") + licenseFile.getPath());
            }

            if (!licenseFile.exists()) {
               getLog().warn(String.format("Unable to acquire the license file(%s), skipping license copying...", licenseFile.getAbsolutePath()));
            }
            else {
               if (verbose) {
                  getLog().info(String.format("Copying license(%s)...", licenseFile.getName()));
               }

               copyFile = new File(project.getBuild().getOutputDirectory() + System.getProperty("file.separator") + licenseFile.getName());

               try {
                  outputStream = new FileOutputStream(copyFile);
               }
               catch (IOException ioException) {
                  throw new MojoExecutionException("Unable to create output license file (" + copyFile.getAbsolutePath() + ")", ioException);
               }

               try {
                  inputStream = new FileInputStream(licenseFile);

                  while ((bytesRead = inputStream.read(buffer)) >= 0) {
                     outputStream.write(buffer, 0, bytesRead);
                  }

                  inputStream.close();
               }
               catch (IOException ioException) {
                  copyFile.delete();
                  throw new MojoExecutionException("Problem in copying output license file (" + copyFile.getAbsolutePath() + ")", ioException);
               }

               try {
                  outputStream.close();
               }
               catch (IOException ioException) {
                  throw new MojoExecutionException("Problem in closing license file (" + licenseFile.getAbsolutePath() + ")", ioException);
               }
            }
         }
      }
   }
}

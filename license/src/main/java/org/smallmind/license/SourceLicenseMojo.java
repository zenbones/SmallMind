package org.smallmind.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.Stencil;

/**
 * @goal generate-license-headers
 * @phase prepare-package
 * @description Generates and/or replaces license headers in source files
 */
public class SourceLicenseMojo extends AbstractMojo {

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
    * @parameter default-value=true
    */
   private boolean includeResources;

   /**
    * @parameter default-value=false
    */
   private boolean includeTests;

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
         FileFilter[] fileFilters;

         if (rule.getLicense() != null) {
            licenseFile = new File(rule.getLicense());
            licenseArray = getFileAsLineArray(licenseFile.isAbsolute() ? licenseFile.getAbsolutePath() : rootProject.getBasedir() + System.getProperty("file.separator") + licenseFile.getPath());

            if ((rule.getFileTypes() == null) || (rule.getFileTypes().length == 0)) {
               throw new MojoExecutionException("No file types were specified for rule(" + rule.getId() + ")");
            }

            fileFilters = new FileFilter[rule.getFileTypes().length];
            for (int count = 0; count < fileFilters.length; count++) {
               fileFilters[count] = new FileTypeFilenameFilter(rule.getFileTypes()[count]);
            }

            processFiles(project.getBuild().getSourceDirectory(), fileFilters);
            processFiles(project.getBuild().getScriptSourceDirectory(), fileFilters);

            if (includeResources) {
               for (Resource resource : project.getBuild().getResources()) {
                  processFiles(resource.getDirectory(), fileFilters);
               }
            }

            if (includeTests) {
               processFiles(project.getBuild().getTestSourceDirectory(), fileFilters);

               if (includeResources) {
                  for (Resource testResource : project.getBuild().getTestResources()) {
                     processFiles(testResource.getDirectory(), fileFilters);
                  }
               }
            }
         }
      }
   }

   private void processFiles (String directoryPath, FileFilter... fileFilters)
      throws MojoExecutionException {

      for (File licensedFile : new LicensedFileIterator(new File(directoryPath), fileFilters)) {
         System.out.println("**************:" + licensedFile.getName());
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

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

         File licenseFile;
         FileFilter[] fileFilters;
         String[] licenseArray;
         boolean stenciled;
         long licenseModTime;

         if (rule.getLicense() != null) {
            licenseFile = new File(rule.getLicense());
            licenseArray = getFileAsLineArray(licenseFile.isAbsolute() ? licenseFile.getAbsolutePath() : rootProject.getBasedir() + System.getProperty("file.separator") + licenseFile.getPath());
            licenseModTime = licenseFile.lastModified();

            if ((rule.getFileTypes() == null) || (rule.getFileTypes().length == 0)) {
               throw new MojoExecutionException("No file types were specified for rule(" + rule.getId() + ")");
            }

            fileFilters = new FileFilter[rule.getFileTypes().length];
            for (int count = 0; count < fileFilters.length; count++) {
               fileFilters[count] = new FileTypeFilenameFilter(rule.getFileTypes()[count]);
            }

            stenciled = false;
            for (Stencil stencil : stencils) {
               if (stencil.getId().equals(rule.getStencilId())) {
                  stenciled = true;

                  processFiles(stencil, licenseArray, licenseModTime, project.getBuild().getSourceDirectory(), fileFilters);
                  processFiles(stencil, licenseArray, licenseModTime, project.getBuild().getScriptSourceDirectory(), fileFilters);

                  if (includeResources) {
                     for (Resource resource : project.getBuild().getResources()) {
                        processFiles(stencil, licenseArray, licenseModTime, resource.getDirectory(), fileFilters);
                     }
                  }

                  if (includeTests) {
                     processFiles(stencil, licenseArray, licenseModTime, project.getBuild().getTestSourceDirectory(), fileFilters);

                     if (includeResources) {
                        for (Resource testResource : project.getBuild().getTestResources()) {
                           processFiles(stencil, licenseArray, licenseModTime, testResource.getDirectory(), fileFilters);
                        }
                     }
                  }
               }
            }

            if (!stenciled) {
               throw new MojoExecutionException("No stencil found with id(" + rule.getStencilId() + ") for rule(" + rule.getId() + ")");
            }
         }
      }
   }

   private void processFiles (Stencil stencil, String[] licenseArray, long licenseModTime, String directoryPath, FileFilter... fileFilters)
      throws MojoExecutionException {

      for (File licensedFile : new LicensedFileIterator(new File(directoryPath), fileFilters)) {

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

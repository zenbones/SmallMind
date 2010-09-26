package org.smallmind.license;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.regex.Pattern;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.JavaDocStencil;
import org.smallmind.license.stencil.Stencil;

/**
 * @goal generate-license-headers
 * @phase prepare-package
 * @description Generates and/or replaces license headers in source files
 */
public class SourceLicenseMojo extends AbstractMojo {

   private static final Stencil[] DEFAULT_STENCILS = new Stencil[] {new JavaDocStencil()};

   private static enum LicenseState {

      FIRST, LAST, COMPLETED, TERMINATED
   }

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

      MavenProject rootProject;
      Stencil[] mergedStencils;
      char[] buffer = new char[8192];

      rootProject = project;
      while (!rootProject.isExecutionRoot()) {
         rootProject = rootProject.getParent();
      }

      mergedStencils = new Stencil[(stencils != null) ? stencils.length + DEFAULT_STENCILS.length : DEFAULT_STENCILS.length];
      System.arraycopy(DEFAULT_STENCILS, 0, mergedStencils, 0, DEFAULT_STENCILS.length);
      if (stencils != null) {
         System.arraycopy(stencils, 0, mergedStencils, DEFAULT_STENCILS.length, stencils.length);
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
            for (Stencil stencil : mergedStencils) {
               if (stencil.getId().equals(rule.getStencilId())) {
                  stenciled = true;

                  processFiles(stencil, licenseArray, licenseModTime, buffer, project.getBuild().getSourceDirectory(), fileFilters);
                  processFiles(stencil, licenseArray, licenseModTime, buffer, project.getBuild().getScriptSourceDirectory(), fileFilters);

                  if (includeResources) {
                     for (Resource resource : project.getBuild().getResources()) {
                        processFiles(stencil, licenseArray, licenseModTime, buffer, resource.getDirectory(), fileFilters);
                     }
                  }

                  if (includeTests) {
                     processFiles(stencil, licenseArray, licenseModTime, buffer, project.getBuild().getTestSourceDirectory(), fileFilters);

                     if (includeResources) {
                        for (Resource testResource : project.getBuild().getTestResources()) {
                           processFiles(stencil, licenseArray, licenseModTime, buffer, testResource.getDirectory(), fileFilters);
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

   private void processFiles (Stencil stencil, String[] licenseArray, long licenseModTime, char[] buffer, String directoryPath, FileFilter... fileFilters)
      throws MojoExecutionException {

      File tempFile;
      BufferedReader fileReader;
      FileWriter fileWriter;
      LicenseState licenseState;
      Pattern skipPattern = null;
      String singleLine = null;
      int charsRead;

      if (stencil.getSkipLines() != null) {
         skipPattern = Pattern.compile(stencil.getSkipLines());
      }

      for (File licensedFile : new LicensedFileIterator(new File(directoryPath), fileFilters)) {
         try {
            fileWriter = new FileWriter(tempFile = new File(licensedFile.getParent() + System.getProperty("file.separator") + "license.temp"));

            try {
               fileReader = new BufferedReader(new FileReader(licensedFile));

               licenseState = (stencil.getFirstLine() != null) ? LicenseState.FIRST : LicenseState.LAST;
               while ((!(licenseState.equals(LicenseState.COMPLETED) || licenseState.equals(LicenseState.TERMINATED))) && ((singleLine = fileReader.readLine()) != null)) {
                  if ((skipPattern == null) || (!skipPattern.matcher(singleLine).matches())) {
                     switch (licenseState) {
                        case FIRST:
                           if (singleLine.length() > 0) {
                              licenseState = singleLine.equals(stencil.getFirstLine()) ? LicenseState.LAST : LicenseState.TERMINATED;
                           }
                           break;
                        case LAST:
                           if ((stencil.getLastLine() != null) && singleLine.equals(stencil.getLastLine())) {
                              licenseState = LicenseState.COMPLETED;
                           }
                           else if ((singleLine.length() > 0) && (!singleLine.startsWith(stencil.getBeforeEachLine()))) {
                              licenseState = LicenseState.TERMINATED;
                           }
                           else if ((singleLine.length() == 0) && stencil.willPrefixBlankLines()) {
                              licenseState = LicenseState.TERMINATED;
                           }
                           break;
                        default:
                           throw new MojoFailureException("Unknown or inappropriate license seek state(" + licenseState.name() + ")");
                     }
                  }
                  else {
                     fileWriter.write(singleLine);
                     fileWriter.write(System.getProperty("file.separator"));
                  }
               }

               if (licenseState.equals(LicenseState.COMPLETED) || ((singleLine != null) && (singleLine.length() == 0))) {
                  do {
                     singleLine = fileReader.readLine();
                  } while ((singleLine != null) && (singleLine.length() == 0));
               }

               for (int count = 0; count < stencil.getBlankLinesBefore(); count++) {
                  fileWriter.write(System.getProperty("file.separator"));
               }

               if (stencil.getFirstLine() != null) {
                  fileWriter.write(stencil.getFirstLine());
                  fileWriter.write(System.getProperty("file.separator"));
               }

               for (String licenseLine : licenseArray) {
                  if ((stencil.getBeforeEachLine() != null) && ((licenseLine.length() > 0) || stencil.willPrefixBlankLines())) {
                     fileWriter.write(stencil.getBeforeEachLine());
                  }
                  fileWriter.write(stencil.getFirstLine());
                  fileWriter.write(System.getProperty("file.separator"));
               }

               if (stencil.getLastLine() != null) {
                  fileWriter.write(stencil.getLastLine());
                  fileWriter.write(System.getProperty("file.separator"));
               }

               for (int count = 0; count < stencil.getBlankLinesAfter(); count++) {
                  fileWriter.write(System.getProperty("file.separator"));
               }

               if (singleLine != null) {
                  fileWriter.write(singleLine);
                  fileWriter.write(System.getProperty("file.separator"));
               }

               while ((charsRead = fileReader.read(buffer)) >= 0) {
                  fileWriter.write(buffer, 0, charsRead);
               }

               fileWriter.close();
               fileReader.close();

               if (!licensedFile.delete()) {
                  throw new MojoFailureException("Unable to delete file(" + licensedFile.getAbsolutePath() + ")");
               }
            }
            catch (Exception exception) {
               tempFile.delete();
               throw new MojoExecutionException("Exception during license processing", exception);
            }

            if (!tempFile.renameTo(licensedFile)) {
               throw new MojoFailureException("Unable to rename temp file(" + tempFile.getAbsolutePath() + ") to processed file(" + licensedFile.getAbsolutePath() + ")");
            }
         }
         catch (MojoExecutionException mojoExecutionException) {
            throw mojoExecutionException;
         }
         catch (Exception exception) {
            throw new MojoExecutionException("Exception during license processing", exception);
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

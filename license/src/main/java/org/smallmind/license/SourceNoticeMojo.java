package org.smallmind.license;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.smallmind.license.stencil.JavaDocStencil;
import org.smallmind.license.stencil.Stencil;

// Generates and/or replaces notice headers in source files
@Mojo(name = "generate-notice-headers", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class SourceNoticeMojo extends AbstractMojo {

  private static enum NoticeState {

    FIRST, LAST, COMPLETED, TERMINATED
  }

  private static final Stencil[] DEFAULT_STENCILS = new Stencil[] {new JavaDocStencil()};
  @Parameter(readonly = true, property = "project")
  private MavenProject project;
  @Parameter
  private Root root;
  @Parameter
  private Stencil[] stencils;
  @Parameter
  private Rule[] rules;
  @Parameter(defaultValue = "false")
  private boolean allowNoticeRemoval;
  @Parameter(defaultValue = "true")
  private boolean includeResources;
  @Parameter(defaultValue = "false")
  private boolean includeTests;
  @Parameter(defaultValue = "false")
  private boolean verbose;

  //TODO: Excludes, Seek/Process Optimization

  @Override
  public void execute ()
    throws MojoExecutionException, MojoFailureException {

    MavenProject rootProject = project;
    Stencil[] mergedStencils;
    char[] buffer = new char[8192];

    while ((root == null) ? !(rootProject.getParent() == null) : !(root.getGroupId().equals(rootProject.getGroupId()) && root.getArtifactId().equals(rootProject.getArtifactId()))) {
      rootProject = rootProject.getParent();
    }

    mergedStencils = new Stencil[(stencils != null) ? stencils.length + DEFAULT_STENCILS.length : DEFAULT_STENCILS.length];
    System.arraycopy(DEFAULT_STENCILS, 0, mergedStencils, 0, DEFAULT_STENCILS.length);
    if (stencils != null) {
      System.arraycopy(stencils, 0, mergedStencils, DEFAULT_STENCILS.length, stencils.length);
    }

    for (Rule rule : rules) {

      PathFilter[] pathFilters;
      String[] noticeArray;
      boolean noticed;
      boolean stenciled;

      if (verbose) {
        getLog().info(String.format("Processing rule(%s)...", rule.getId()));
      }

      noticed = true;
      if (rule.getNotice() == null) {
        if (!allowNoticeRemoval) {
          throw new MojoExecutionException("No notice was provided for rule(" + rule.getId() + "), but notice removal has not been enabled(allowNoticeRemoval = false)");
        }

        noticeArray = null;
      } else {

        Path noticeFile;

        noticeFile = Paths.get(rule.getNotice());
        if ((noticeArray = getFileAsLineArray(noticeFile.isAbsolute() ? noticeFile : rootProject.getBasedir().toPath().resolve(noticeFile))) == null) {
          noticed = false;
        }
      }

      if (!noticed) {
        getLog().warn(String.format("Unable to acquire the notice file(%s), skipping notice updating...", rule.getNotice()));
      } else {
        if ((rule.getFileTypes() == null) || (rule.getFileTypes().length == 0)) {
          throw new MojoExecutionException("No file types were specified for rule(" + rule.getId() + ")");
        }

        pathFilters = new PathFilter[rule.getFileTypes().length];
        for (int count = 0; count < pathFilters.length; count++) {
          pathFilters[count] = new PathTypeFilenameFilter(rule.getFileTypes()[count]);
        }

        stenciled = false;
        for (Stencil stencil : mergedStencils) {
          if (stencil.getId().equals(rule.getStencilId())) {
            stenciled = true;

            updateNotice(stencil, noticeArray, buffer, project.getBuild().getSourceDirectory(), pathFilters);
            updateNotice(stencil, noticeArray, buffer, project.getBuild().getScriptSourceDirectory(), pathFilters);

            if (includeResources) {
              for (Resource resource : project.getBuild().getResources()) {
                updateNotice(stencil, noticeArray, buffer, resource.getDirectory(), pathFilters);
              }
            }

            if (includeTests) {
              updateNotice(stencil, noticeArray, buffer, project.getBuild().getTestSourceDirectory(), pathFilters);

              if (includeResources) {
                for (Resource testResource : project.getBuild().getTestResources()) {
                  updateNotice(stencil, noticeArray, buffer, testResource.getDirectory(), pathFilters);
                }
              }
            }

            break;
          }
        }

        if (!stenciled) {
          throw new MojoExecutionException("No stencil found with id(" + rule.getStencilId() + ") for rule(" + rule.getId() + ")");
        }
      }
    }
  }

  private void updateNotice (Stencil stencil, String[] noticeArray, char[] buffer, String directory, PathFilter... pathFilters)
    throws MojoFailureException {

    Path directoryPath = Paths.get(directory);

    if (Files.isDirectory(directoryPath)) {

      final Pattern skipPattern;

      if (stencil.getSkipLines() != null) {
        skipPattern = Pattern.compile(stencil.getSkipLines());
      } else {
        skipPattern = null;
      }

      try (Stream<Path> pathStream = Files.walk(directoryPath)) {
        try {
          pathStream.forEach((licensedPath) -> {
            if (Files.isRegularFile(licensedPath) && accept(licensedPath, pathFilters)) {

              Path tempPath;

              if (verbose) {
                getLog().info(String.format(((noticeArray == null) ? "Removing" : "Updating") + " license notice for file(%s)...", licensedPath));
              }

              try {
                try (BufferedWriter fileWriter = Files.newBufferedWriter(tempPath = licensedPath.getParent().resolve("license.temp"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                  try (BufferedReader fileReader = Files.newBufferedReader(licensedPath)) {

                    String unprocessedLine;
                    int charsRead;

                    unprocessedLine = seekNotice(stencil, skipPattern, fileReader, fileWriter);

                    if (noticeArray != null) {
                      applyNotice(stencil, noticeArray, fileWriter);
                    }

                    if (unprocessedLine != null) {
                      fileWriter.write(unprocessedLine);
                      fileWriter.write(System.getProperty("line.separator"));
                    }

                    while ((charsRead = fileReader.read(buffer)) >= 0) {
                      fileWriter.write(buffer, 0, charsRead);
                    }
                  }
                }

                Files.move(tempPath, licensedPath, StandardCopyOption.REPLACE_EXISTING);
              } catch (IOException ioException) {
                throw new WrappingException(new MojoFailureException(ioException.getMessage(), ioException));
              } catch (MojoFailureException mojoFailureException) {
                throw new WrappingException(mojoFailureException);
              }
            }
          });
        } catch (WrappingException wrappingException) {
          throw (MojoFailureException)wrappingException.getCause();
        }
      } catch (IOException ioException) {
        throw new MojoFailureException(ioException.getMessage(), ioException);
      }
    }
  }

  private boolean accept (Path path, PathFilter... pathFilters) {

    if ((pathFilters != null) && (pathFilters.length > 0)) {
      for (PathFilter pathFilter : pathFilters) {
        if (pathFilter.accept(path)) {

          return true;
        }
      }

      return false;
    }

    return true;
  }

  private String[] getFileAsLineArray (Path noticePath) {

    LinkedList<String> lineList;
    String[] lineArray;

    try (BufferedReader noticeReader = Files.newBufferedReader(noticePath)) {

      String singleLine;

      lineList = new LinkedList<>();
      while ((singleLine = noticeReader.readLine()) != null) {
        lineList.add(singleLine);
      }
    } catch (IOException ioException) {

      return null;
    }

    lineArray = new String[lineList.size()];
    lineList.toArray(lineArray);

    return lineArray;
  }

  private String seekNotice (Stencil stencil, Pattern skipPattern, BufferedReader fileReader, BufferedWriter fileWriter)
    throws IOException, MojoFailureException {

    NoticeState noticeState;
    String singleLine = null;
    String generalPrefix = (stencil.getBeforeEachLine() != null) ? stencil.getBeforeEachLine() : "";
    int whitespaceIndex = generalPrefix.length();

    while ((whitespaceIndex > 0) && Character.isWhitespace(generalPrefix.charAt(whitespaceIndex - 1))) {
      whitespaceIndex--;
    }
    generalPrefix = generalPrefix.substring(0, whitespaceIndex);

    noticeState = (stencil.getFirstLine() != null) ? NoticeState.FIRST : NoticeState.LAST;
    while ((!(noticeState.equals(NoticeState.COMPLETED) || noticeState.equals(NoticeState.TERMINATED))) && ((singleLine = fileReader.readLine()) != null)) {
      if ((skipPattern == null) || (!skipPattern.matcher(singleLine).matches())) {
        switch (noticeState) {
          case FIRST:
            if (singleLine.length() > 0) {
              noticeState = singleLine.equals(stencil.getFirstLine()) ? NoticeState.LAST : NoticeState.TERMINATED;
            }
            break;
          case LAST:
            if ((stencil.getLastLine() != null) && singleLine.equals(stencil.getLastLine())) {
              noticeState = NoticeState.COMPLETED;
            } else if ((singleLine.length() > 0) && (!singleLine.startsWith(generalPrefix))) {
              noticeState = NoticeState.TERMINATED;
            } else if ((singleLine.length() == 0) && stencil.willPrefixBlankLines()) {
              noticeState = NoticeState.TERMINATED;
            }
            break;
          default:
            throw new MojoFailureException("Unknown or inappropriate notice seeking state(" + noticeState.name() + ")");
        }
      } else {
        fileWriter.write(singleLine);
        fileWriter.write(System.getProperty("line.separator"));
      }
    }

    if (noticeState.equals(NoticeState.COMPLETED) || ((singleLine != null) && (singleLine.length() == 0))) {
      do {
        singleLine = fileReader.readLine();
      } while ((singleLine != null) && (singleLine.length() == 0));
    }

    return singleLine;
  }

  private void applyNotice (Stencil stencil, String[] noticeArray, BufferedWriter fileWriter)
    throws IOException {

    for (int count = 0; count < stencil.getBlankLinesBefore(); count++) {
      fileWriter.write(System.getProperty("line.separator"));
    }

    if (stencil.getFirstLine() != null) {
      fileWriter.write(stencil.getFirstLine());
      fileWriter.write(System.getProperty("line.separator"));
    }

    for (String noticeLine : noticeArray) {
      if ((stencil.getBeforeEachLine() != null) && ((noticeLine.length() > 0) || stencil.willPrefixBlankLines())) {
        fileWriter.write(stencil.getBeforeEachLine());
      }
      fileWriter.write(noticeLine);
      fileWriter.write(System.getProperty("line.separator"));
    }

    if (stencil.getLastLine() != null) {
      fileWriter.write(stencil.getLastLine());
      fileWriter.write(System.getProperty("line.separator"));
    }

    for (int count = 0; count < stencil.getBlankLinesAfter(); count++) {
      fileWriter.write(System.getProperty("line.separator"));
    }
  }

  private class WrappingException extends RuntimeException {

    private WrappingException (MojoFailureException mojoFailureException) {

      super(mojoFailureException);
    }
  }
}

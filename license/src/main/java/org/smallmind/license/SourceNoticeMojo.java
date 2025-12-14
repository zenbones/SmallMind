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

/**
 * Mojo that inserts or removes license notices in source and resource files according to configured rules and
 * formatting stencils.
 */
@Mojo(name = "generate-notice-headers", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class SourceNoticeMojo extends AbstractMojo {

  /**
   * Tracks the position of the notice while scanning an existing file.
   */
  private enum NoticeState {

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
  @Parameter(defaultValue = "UNIX")
  private LineEndings lineEndings;
  @Parameter(defaultValue = "false")
  private boolean allowNoticeRemoval;
  @Parameter(defaultValue = "true")
  private boolean includeResources;
  @Parameter(defaultValue = "false")
  private boolean includeTests;
  @Parameter(defaultValue = "false")
  private boolean verbose;

  //TODO: Excludes, Seek/Process Optimization

  /**
   * Executes the mojo by applying each configured rule to project sources, scripts, and optionally resources and test
   * assets. Notices are added, replaced, or removed depending on the rule configuration.
   *
   * @throws MojoExecutionException if rule validation fails or a stencil cannot be found
   * @throws MojoFailureException   if notice processing encounters an unrecoverable error
   */
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

  /**
   * Walks the provided directory and applies or removes the notice text for every file accepted by the supplied path
   * filters using the given stencil.
   *
   * @param stencil     the stencil that formats notice text for target files
   * @param noticeArray the notice contents split into lines, or {@code null} to remove existing notices
   * @param buffer      reusable character buffer for stream copying
   * @param directory   the directory root to traverse
   * @param pathFilters file acceptance criteria determining which files are processed
   * @throws MojoFailureException if I/O issues occur while updating files
   */
  private void updateNotice (Stencil stencil, String[] noticeArray, char[] buffer, String directory, PathFilter... pathFilters)
    throws MojoFailureException {

    LineTerminator lineTerminator = new LineTerminator(lineEndings);
    Path directoryPath = Paths.get(directory);

    if (Files.isDirectory(directoryPath)) {

      final Pattern skipPattern;

      if (stencil.getSkipLinePattern() != null) {
        skipPattern = Pattern.compile(stencil.getSkipLinePattern());
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

                    unprocessedLine = seekNotice(stencil, skipPattern, fileReader, fileWriter, lineTerminator);

                    if (noticeArray != null) {
                      applyNotice(stencil, noticeArray, fileWriter, lineTerminator);
                    }

                    if (unprocessedLine != null) {
                      fileWriter.write(unprocessedLine);
                      fileWriter.write(lineTerminator.end());
                    }

                    while ((charsRead = fileReader.read(buffer)) >= 0) {
                      fileWriter.write(buffer, 0, charsRead);
                    }
                  }
                }

                Files.move(tempPath, licensedPath, StandardCopyOption.REPLACE_EXISTING);
              } catch (IOException ioException) {
                throw new WrappedException(new MojoFailureException(ioException.getMessage(), ioException));
              } catch (MojoFailureException mojoFailureException) {
                throw new WrappedException(mojoFailureException);
              }
            }
          });
        } catch (WrappedException wrappingException) {
          throw wrappingException.convert(MojoFailureException.class);
        }
      } catch (IOException ioException) {
        throw new MojoFailureException(ioException.getMessage(), ioException);
      }
    }
  }

  /**
   * Evaluates the supplied path against the configured filters.
   *
   * @param path        the path to evaluate
   * @param pathFilters filters determining acceptable files
   * @return {@code true} if no filters are provided or any filter accepts the path; otherwise {@code false}
   */
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

  /**
   * Reads the file at the provided path into an array of lines.
   *
   * @param noticePath the path to the notice file
   * @return an array of file lines, or {@code null} if the file cannot be read
   */
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

  /**
   * Scans through the input file to locate and skip over an existing notice, writing any skipped content to the
   * provided writer as needed.
   *
   * @param stencil        the stencil describing expected notice boundaries
   * @param skipPattern    an optional pattern to bypass lines that should not be considered part of the notice
   * @param fileReader     source reader for the file being processed
   * @param fileWriter     destination writer receiving preserved content
   * @param lineTerminator helper supplying the correct line separator
   * @return the first line after the located notice, or {@code null} if the end of file is reached
   * @throws IOException          if an I/O error occurs while reading or writing
   * @throws MojoFailureException if an unexpected notice seeking state is encountered
   */
  private String seekNotice (Stencil stencil, Pattern skipPattern, BufferedReader fileReader, BufferedWriter fileWriter, LineTerminator lineTerminator)
    throws IOException, MojoFailureException {

    NoticeState noticeState;
    String singleLine = null;

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
            } else if ((singleLine.length() > 0) && (!(singleLine.equals(stencil.getBlankLinePrefix()) || ((stencil.getLinePrefix() != null) && singleLine.startsWith(stencil.getLinePrefix()))))) {
              noticeState = NoticeState.TERMINATED;
            } else if ((singleLine.length() == 0) && (stencil.getBlankLinePrefix() == null)) {
              noticeState = NoticeState.TERMINATED;
            }
            break;
          default:
            throw new MojoFailureException("Unknown or inappropriate notice seeking state(" + noticeState.name() + ")");
        }
      } else {
        fileWriter.write(singleLine);
        fileWriter.write(lineTerminator.end());
      }
    }

    if (noticeState.equals(NoticeState.COMPLETED) || ((singleLine != null) && (singleLine.length() == 0))) {
      do {
        singleLine = fileReader.readLine();
      } while ((singleLine != null) && (singleLine.length() == 0));
    }

    return singleLine;
  }

  /**
   * Writes the notice text to the output using the supplied stencil formatting and line termination.
   *
   * @param stencil        the stencil describing how the notice should be delimited and prefixed
   * @param noticeArray    the notice text lines to write
   * @param fileWriter     the destination writer receiving the notice
   * @param lineTerminator helper supplying the correct line separator
   * @throws IOException if writing to the output fails
   */
  private void applyNotice (Stencil stencil, String[] noticeArray, BufferedWriter fileWriter, LineTerminator lineTerminator)
    throws IOException {

    for (int count = 0; count < stencil.getBlankLinesBefore(); count++) {
      fileWriter.write(lineTerminator.end());
    }

    if (stencil.getFirstLine() != null) {
      fileWriter.write(stencil.getFirstLine());
      fileWriter.write(lineTerminator.end());
    }

    for (String noticeLine : noticeArray) {
      if (noticeLine.length() == 0) {
        if (stencil.getBlankLinePrefix() != null) {
          fileWriter.write(stencil.getBlankLinePrefix());
        }
      } else {
        if (stencil.getLinePrefix() != null) {
          fileWriter.write(stencil.getLinePrefix());
        }
        fileWriter.write(noticeLine);
      }
      fileWriter.write(lineTerminator.end());
    }

    if (stencil.getLastLine() != null) {
      fileWriter.write(stencil.getLastLine());
      fileWriter.write(lineTerminator.end());
    }

    for (int count = 0; count < stencil.getBlankLinesAfter(); count++) {
      fileWriter.write(lineTerminator.end());
    }
  }
}

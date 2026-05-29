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
 * Maven Mojo that inserts, replaces, or removes license notice headers in source and resource files.
 *
 * <p>Bound to the {@code process-sources} lifecycle phase under the goal name
 * {@code generate-notice-headers}. For each configured {@link Rule}, the mojo walks the project's
 * source, script, and optionally resource directories and rewrites every file whose name matches
 * one of the rule's file-type patterns and matches none of its exclude patterns. Existing notices
 * are detected and replaced atomically by writing to a temporary file and then moving it over the
 * original.
 *
 * <p>Notices are formatted using the {@link org.smallmind.license.stencil.Stencil} identified by
 * {@link Rule#getStencilId()}. The built-in {@link JavaDocStencil} is always available; additional
 * stencils may be supplied via the {@code stencils} configuration parameter.
 */
@Mojo(name = "generate-notice-headers", defaultPhase = LifecyclePhase.PROCESS_SOURCES, threadSafe = true)
public class SourceNoticeMojo extends AbstractMojo {

  /**
   * Tracks the mojo's progress through an existing notice block while scanning a file.
   *
   * <ul>
   *   <li>{@link #FIRST} – still searching for the opening delimiter line.</li>
   *   <li>{@link #LAST} – inside the block; scanning for the closing delimiter or a non-notice
   *       line.</li>
   *   <li>{@link #COMPLETED} – the closing delimiter was found; the notice has been fully
   *       consumed.</li>
   *   <li>{@link #TERMINATED} – a non-notice line was encountered before a closing delimiter;
   *       no complete existing notice is present.</li>
   * </ul>
   */
  private enum NoticeState {

    /**
     * Seeking the opening stencil delimiter.
     */
    FIRST,

    /**
     * Inside the notice block; seeking the closing delimiter or a non-notice line.
     */
    LAST,

    /**
     * The closing delimiter was found; notice fully consumed.
     */
    COMPLETED,

    /**
     * A non-notice line ended the search; no existing notice present.
     */
    TERMINATED
  }

  private static final Stencil[] DEFAULT_STENCILS = new Stencil[] {new JavaDocStencil()};

  /**
   * The current Maven project, supplied by Maven at execution time.
   */
  @Parameter(readonly = true, property = "project")
  private MavenProject project;

  /**
   * Optional root project identifier. When set, notice file paths are resolved relative to the
   * identified ancestor module rather than the top-most Maven parent.
   */
  @Parameter
  private Root root;

  /**
   * Additional stencils available to rules, merged with the built-in {@link JavaDocStencil}.
   */
  @Parameter
  private Stencil[] stencils;

  /**
   * Rules describing which file types should receive which notice text and formatting stencil.
   * At least one rule is required.
   */
  @Parameter(required = true)
  private Rule[] rules;

  /**
   * Line-ending style used when writing generated notice content. Defaults to {@code UNIX}.
   */
  @Parameter(defaultValue = "UNIX")
  private LineEndings lineEndings;

  /**
   * When {@code true}, a rule with no {@code notice} file removes an existing top-of-file notice
   * instead of failing the build.
   */
  @Parameter(defaultValue = "false")
  private boolean allowNoticeRemoval;

  /**
   * When {@code true}, the module's configured main resource directories are included when
   * applying rules.
   */
  @Parameter(defaultValue = "true")
  private boolean includeResources;

  /**
   * When {@code true}, test source directories (and, if {@code includeResources} is also
   * {@code true}, test resource directories) are included when applying rules.
   */
  @Parameter(defaultValue = "false")
  private boolean includeTests;

  /**
   * When {@code true}, informational log messages are emitted for each rule and each file updated.
   */
  @Parameter(defaultValue = "false")
  private boolean verbose;

  //TODO: Seek/Process Optimization

  /**
   * Applies each configured rule to the project's source, script, and optionally resource
   * directories. For each file whose name matches a rule's {@code fileTypes} pattern and does not
   * match any of its {@code excludes} patterns, an existing notice is detected and removed; if the
   * rule supplies a notice file, the new notice is then written using the rule's stencil.
   *
   * @throws MojoExecutionException if a rule specifies no file types, supplies no notice when
   *                                removal is disabled, or references an unknown stencil id
   * @throws MojoFailureException   if an I/O error occurs while processing a file
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

      PathFilter[] includeFilters;
      PathFilter[] excludeFilters = new PathFilter[(rule.getExcludes() == null) ? 0 : rule.getExcludes().length];
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

        includeFilters = new PathFilter[rule.getFileTypes().length];
        for (int count = 0; count < includeFilters.length; count++) {
          includeFilters[count] = new PathTypeFilenameFilter(rule.getFileTypes()[count]);
        }

        if (rule.getExcludes() != null) {
          for (int count = 0; count < excludeFilters.length; count++) {
            excludeFilters[count] = new PathTypeFilenameFilter(rule.getExcludes()[count]);
          }
        }

        stenciled = false;
        for (Stencil stencil : mergedStencils) {
          if (stencil.getId().equals(rule.getStencilId())) {
            stenciled = true;

            updateNotice(stencil, noticeArray, buffer, project.getBuild().getSourceDirectory(), includeFilters, excludeFilters);
            updateNotice(stencil, noticeArray, buffer, project.getBuild().getScriptSourceDirectory(), includeFilters, excludeFilters);

            if (includeResources) {
              for (Resource resource : project.getBuild().getResources()) {
                updateNotice(stencil, noticeArray, buffer, resource.getDirectory(), includeFilters, excludeFilters);
              }
            }

            if (includeTests) {
              updateNotice(stencil, noticeArray, buffer, project.getBuild().getTestSourceDirectory(), includeFilters, excludeFilters);

              if (includeResources) {
                for (Resource testResource : project.getBuild().getTestResources()) {
                  updateNotice(stencil, noticeArray, buffer, testResource.getDirectory(), includeFilters, excludeFilters);
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
   * Walks the given directory and applies or removes the notice for every file accepted by the
   * supplied path filters, using the given stencil for formatting.
   *
   * @param stencil        the stencil that controls how the notice is delimited and prefixed
   * @param noticeArray    the notice text split into individual lines, or {@code null} to remove an
   *                       existing notice without writing a replacement
   * @param buffer         reusable character buffer used when copying remaining file content
   * @param directory      root of the directory tree to traverse
   * @param includeFilters acceptance criteria that determine which files are processed
   * @param excludeFilters criteria that veto an otherwise-accepted file; an empty array excludes nothing
   * @throws MojoFailureException if an I/O error occurs while reading or writing a file
   */
  private void updateNotice (Stencil stencil, String[] noticeArray, char[] buffer, String directory, PathFilter[] includeFilters, PathFilter... excludeFilters)
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
            if (Files.isRegularFile(licensedPath) && accept(licensedPath, true, includeFilters) && (!accept(licensedPath, false, excludeFilters))) {

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
   * @param path               the path to evaluate; must not be {@code null}
   * @param emptyFilterDefault the value returned when {@code pathFilters} is empty or {@code null}
   * @param pathFilters        filters that determine acceptable files
   * @return {@code true} if any filter accepts the path; {@code false} if all filters reject it;
   * {@code emptyFilterDefault} if no filters are provided
   */
  private boolean accept (Path path, boolean emptyFilterDefault, PathFilter... pathFilters) {

    if ((pathFilters != null) && (pathFilters.length > 0)) {
      for (PathFilter pathFilter : pathFilters) {
        if (pathFilter.accept(path)) {

          return true;
        }
      }

      return false;
    }

    return emptyFilterDefault;
  }

  /**
   * Reads the file at the given path into an array of lines.
   *
   * @param noticePath path to the notice text file
   * @return an array where each element is one line of the file, or {@code null} if the file
   * cannot be opened or read
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
   * Scans the input file to locate and consume an existing notice block, writing any skipped
   * lines to the output writer. Returns the first non-notice, non-blank line following the block
   * so the caller can write it before copying the remainder of the file.
   *
   * @param stencil        the stencil describing expected notice delimiters and prefixes
   * @param skipPattern    compiled pattern for lines that precede the notice and should be
   *                       forwarded verbatim, or {@code null} to skip none
   * @param fileReader     source reader for the file being processed
   * @param fileWriter     destination writer receiving lines that precede or follow the notice
   * @param lineTerminator helper that supplies the correct line separator
   * @return the first non-blank line after the notice block, or {@code null} if the end of file
   * is reached
   * @throws IOException          if an I/O error occurs while reading from or writing to the file
   * @throws MojoFailureException if the notice-seeking state machine reaches an unexpected state
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
            if (!singleLine.isEmpty()) {
              noticeState = singleLine.equals(stencil.getFirstLine()) ? NoticeState.LAST : NoticeState.TERMINATED;
            }
            break;
          case LAST:
            if ((stencil.getLastLine() != null) && singleLine.equals(stencil.getLastLine())) {
              noticeState = NoticeState.COMPLETED;
            } else if ((!singleLine.isEmpty()) && (!(singleLine.equals(stencil.getBlankLinePrefix()) || ((stencil.getLinePrefix() != null) && singleLine.startsWith(stencil.getLinePrefix()))))) {
              noticeState = NoticeState.TERMINATED;
            } else if ((singleLine.isEmpty()) && (stencil.getBlankLinePrefix() == null)) {
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

    if (noticeState.equals(NoticeState.COMPLETED) || ((singleLine != null) && (singleLine.isEmpty()))) {
      do {
        singleLine = fileReader.readLine();
      } while ((singleLine != null) && (singleLine.isEmpty()));
    }

    return singleLine;
  }

  /**
   * Writes the complete notice block to the output, applying the stencil's delimiter lines,
   * per-line prefixes, and surrounding blank-line padding.
   *
   * @param stencil        the stencil that controls delimiter and prefix formatting
   * @param noticeArray    the notice text lines to embed; must not be {@code null}
   * @param fileWriter     destination writer receiving the formatted notice
   * @param lineTerminator helper that supplies the correct line separator
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
      if (noticeLine.isEmpty()) {
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

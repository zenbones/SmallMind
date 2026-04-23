package org.smallmind.license;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * {@link PathFilter} implementation that accepts paths whose file name matches a glob-style pattern.
 *
 * <p>The pattern is translated to a regular expression once at construction time via
 * {@link FileTypeRegExTranslator}. Matching is performed against the last path component only,
 * not the full path string.
 */
public class PathTypeFilenameFilter implements PathFilter {

  private final Pattern namePattern;

  /**
   * Builds a filter that matches file names against the supplied glob-style pattern.
   *
   * @param name a glob-style file name pattern, for example {@code "*.java"} or {@code "*.xml"};
   *             must not be {@code null}
   */
  public PathTypeFilenameFilter (String name) {

    namePattern = Pattern.compile(FileTypeRegExTranslator.translate(name));
  }

  /**
   * Returns {@code true} when the file name of the given path matches the compiled pattern.
   *
   * @param path the path whose file name component is tested; must not be {@code null}
   * @return {@code true} if the file name matches the configured pattern; {@code false} otherwise
   */
  public boolean accept (Path path) {

    return namePattern.matcher(path.getFileName().toString()).matches();
  }
}

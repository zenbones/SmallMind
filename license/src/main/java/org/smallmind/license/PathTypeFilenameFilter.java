package org.smallmind.license;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Filters paths based on a file name pattern, translating glob-like syntax into regular expressions.
 */
public class PathTypeFilenameFilter implements PathFilter {

  private final Pattern namePattern;

  /**
   * Creates a filter that matches file names against the supplied glob-like pattern.
   *
   * @param name the file name pattern, supporting wildcards such as {@code *} and {@code ?}
   */
  public PathTypeFilenameFilter (String name) {

    namePattern = Pattern.compile(FileTypeRegExTranslator.translate(name));
  }

  /**
   * Checks whether the file name for the provided path matches the configured pattern.
   *
   * @param path the path whose file name should be matched
   * @return {@code true} when the name matches the translated pattern, otherwise {@code false}
   */
  public boolean accept (Path path) {

    return namePattern.matcher(path.getFileName().toString()).matches();
  }
}

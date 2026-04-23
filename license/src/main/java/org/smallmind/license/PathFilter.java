package org.smallmind.license;

import java.nio.file.Path;

/**
 * Strategy for deciding whether a given {@link Path} should be included in notice processing.
 */
public interface PathFilter {

  /**
   * Tests whether the supplied path meets the filter's acceptance criteria.
   *
   * @param path the candidate path to evaluate; never {@code null}
   * @return {@code true} if the path should be processed; {@code false} to skip it
   */
  boolean accept (Path path);
}

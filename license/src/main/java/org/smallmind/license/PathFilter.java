package org.smallmind.license;

import java.nio.file.Path;

/**
 * Strategy interface for determining whether a given {@link Path} should be processed.
 */
public interface PathFilter {

  /**
   * Indicates whether the supplied path meets the criteria defined by the filter.
   *
   * @param path the path to evaluate
   * @return {@code true} if the path should be accepted for processing, otherwise {@code false}
   */
  boolean accept (Path path);
}

package org.smallmind.license;

/**
 * Supported line ending styles that may be applied when writing generated notice content.
 */
public enum LineEndings {

  /**
   * Always use a single line feed ({@code \n}) regardless of platform.
   */
  UNIX,
  /**
   * Use the platform default line separator as returned from {@code System.getProperty("line.separator")}.
   */
  SYSTEM
}

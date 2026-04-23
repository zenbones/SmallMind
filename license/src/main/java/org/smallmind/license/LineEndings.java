package org.smallmind.license;

/**
 * Enumerates the line-ending styles that can be applied when writing generated notice content.
 */
public enum LineEndings {

  /**
   * Always emit a Unix line feed ({@code \n}), regardless of the host operating system.
   */
  UNIX,

  /**
   * Emit the host platform's default line separator as returned by
   * {@code System.getProperty("line.separator")}.
   */
  SYSTEM
}

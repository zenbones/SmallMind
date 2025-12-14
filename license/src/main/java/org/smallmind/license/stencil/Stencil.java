package org.smallmind.license.stencil;

/**
 * Describes how a notice should be formatted and delimited when inserted into source files.
 */
public class Stencil {

  private String id;
  private String skipLinePattern;
  private String firstLine;
  private String lastLine;
  private String linePrefix;
  private String blankLinePrefix;
  private int blankLinesBefore;
  private int blankLinesAfter;

  /**
   * Returns the identifier used to reference this stencil.
   *
   * @return the stencil identifier
   */
  public String getId () {

    return id;
  }

  /**
   * Sets the identifier used to reference this stencil.
   *
   * @param id the stencil identifier
   */
  public void setId (String id) {

    this.id = id;
  }

  /**
   * Pattern describing lines to skip while searching for an existing notice.
   *
   * @return the skip line pattern, or {@code null} if no skipping is required
   */
  public String getSkipLinePattern () {

    return skipLinePattern;
  }

  /**
   * Defines the pattern of lines that should be skipped when seeking a notice.
   *
   * @param skipLinePattern the regular expression to match lines that should be ignored
   */
  public void setSkipLinePattern (String skipLinePattern) {

    this.skipLinePattern = skipLinePattern;
  }

  /**
   * Returns the first delimiter line expected at the start of a notice block.
   *
   * @return the first line string, or {@code null} if no explicit first line is required
   */
  public String getFirstLine () {

    return firstLine;
  }

  /**
   * Sets the first delimiter line expected at the start of a notice block.
   *
   * @param firstLine the first line string, or {@code null} when no explicit first line is used
   */
  public void setFirstLine (String firstLine) {

    this.firstLine = firstLine;
  }

  /**
   * Returns the trailing delimiter line that should close a notice block.
   *
   * @return the last line string, or {@code null} when no closing delimiter is required
   */
  public String getLastLine () {

    return lastLine;
  }

  /**
   * Sets the trailing delimiter line that should close a notice block.
   *
   * @param lastLine the closing delimiter string, or {@code null} if none is required
   */
  public void setLastLine (String lastLine) {

    this.lastLine = lastLine;
  }

  /**
   * Returns the prefix applied to each non-blank notice line.
   *
   * @return the notice line prefix, or {@code null} to omit a prefix
   */
  public String getLinePrefix () {

    return linePrefix;
  }

  /**
   * Defines the prefix applied to each non-blank notice line.
   *
   * @param linePrefix the notice line prefix, or {@code null} to omit a prefix
   */
  public void setLinePrefix (String linePrefix) {

    this.linePrefix = linePrefix;
  }

  /**
   * Returns the prefix that should be written when emitting blank lines inside the notice.
   *
   * @return the blank line prefix, or {@code null} to leave blank lines empty
   */
  public String getBlankLinePrefix () {

    return blankLinePrefix;
  }

  /**
   * Defines the prefix that should be written when emitting blank lines inside the notice.
   *
   * @param blankLinePrefix the prefix for blank lines, or {@code null} to leave blanks empty
   */
  public void setBlankLinePrefix (String blankLinePrefix) {

    this.blankLinePrefix = blankLinePrefix;
  }

  /**
   * Returns the number of blank lines that should precede the notice content.
   *
   * @return the count of blank lines before the notice
   */
  public int getBlankLinesBefore () {

    return blankLinesBefore;
  }

  /**
   * Sets the number of blank lines that should precede the notice content.
   *
   * @param blankLinesBefore the number of blank lines to write before the notice
   */
  public void setBlankLinesBefore (int blankLinesBefore) {

    this.blankLinesBefore = blankLinesBefore;
  }

  /**
   * Returns the number of blank lines that should follow the notice content.
   *
   * @return the count of blank lines after the notice
   */
  public int getBlankLinesAfter () {

    return blankLinesAfter;
  }

  /**
   * Sets the number of blank lines that should follow the notice content.
   *
   * @param blankLinesAfter the number of blank lines to write after the notice
   */
  public void setBlankLinesAfter (int blankLinesAfter) {

    this.blankLinesAfter = blankLinesAfter;
  }
}

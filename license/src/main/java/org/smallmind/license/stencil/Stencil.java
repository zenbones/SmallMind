package org.smallmind.license.stencil;

/**
 * Describes the formatting rules used to wrap a notice inside a source file.
 *
 * <p>A stencil defines the opening and closing delimiter lines, the per-line prefix written before
 * each notice line, an alternate prefix for blank lines, and optional padding written before and
 * after the notice block. Subclasses may override individual getters to return fixed values.
 * {@link StaticStencil} additionally makes all setters throw {@link UnsupportedOperationException}
 * to enforce immutability.
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
   * Returns the identifier used to reference this stencil from a {@link org.smallmind.license.Rule}.
   *
   * @return the stencil id; may be {@code null} if not yet assigned
   */
  public String getId () {

    return id;
  }

  /**
   * Sets the identifier used to reference this stencil.
   *
   * @param id the stencil id to assign
   */
  public void setId (String id) {

    this.id = id;
  }

  /**
   * Returns a regular expression matched against leading file lines that should be skipped before
   * the mojo searches for an existing notice block.
   *
   * <p>Useful for files that begin with mandatory lines such as shebang or encoding declarations.
   *
   * @return the skip-line regular expression, or {@code null} if no lines should be skipped
   */
  public String getSkipLinePattern () {

    return skipLinePattern;
  }

  /**
   * Sets the regular expression used to identify leading file lines that should be skipped.
   *
   * @param skipLinePattern a regular expression applied to leading lines, or {@code null} to
   *                        disable skipping
   */
  public void setSkipLinePattern (String skipLinePattern) {

    this.skipLinePattern = skipLinePattern;
  }

  /**
   * Returns the delimiter written as the first line of the notice block.
   *
   * <p>When non-{@code null}, this string is also used to detect and consume existing notices so
   * they can be replaced or removed.
   *
   * @return the opening delimiter, or {@code null} when no explicit first line is required
   */
  public String getFirstLine () {

    return firstLine;
  }

  /**
   * Sets the delimiter written as the first line of the notice block.
   *
   * @param firstLine the opening delimiter string, or {@code null} when none is required
   */
  public void setFirstLine (String firstLine) {

    this.firstLine = firstLine;
  }

  /**
   * Returns the delimiter written as the last line of the notice block.
   *
   * @return the closing delimiter, or {@code null} when no explicit last line is required
   */
  public String getLastLine () {

    return lastLine;
  }

  /**
   * Sets the delimiter written as the last line of the notice block.
   *
   * @param lastLine the closing delimiter string, or {@code null} when none is required
   */
  public void setLastLine (String lastLine) {

    this.lastLine = lastLine;
  }

  /**
   * Returns the string prepended to each non-blank notice line.
   *
   * @return the line prefix, or {@code null} to write notice lines without a prefix
   */
  public String getLinePrefix () {

    return linePrefix;
  }

  /**
   * Sets the string prepended to each non-blank notice line.
   *
   * @param linePrefix the prefix string, or {@code null} to omit a prefix
   */
  public void setLinePrefix (String linePrefix) {

    this.linePrefix = linePrefix;
  }

  /**
   * Returns the string written in place of blank notice lines.
   *
   * <p>When {@code null}, blank lines inside the notice are emitted as truly empty lines. When
   * set, this value is written instead so comment blocks remain properly decorated (for example,
   * {@code " *"} in a JavaDoc block).
   *
   * @return the blank-line prefix, or {@code null} to leave blank lines empty
   */
  public String getBlankLinePrefix () {

    return blankLinePrefix;
  }

  /**
   * Sets the string written in place of blank lines inside the notice block.
   *
   * @param blankLinePrefix the prefix for blank notice lines, or {@code null} for empty lines
   */
  public void setBlankLinePrefix (String blankLinePrefix) {

    this.blankLinePrefix = blankLinePrefix;
  }

  /**
   * Returns the number of blank lines written immediately before the notice block.
   *
   * @return the pre-notice blank line count; {@code 0} means no padding
   */
  public int getBlankLinesBefore () {

    return blankLinesBefore;
  }

  /**
   * Sets the number of blank lines written immediately before the notice block.
   *
   * @param blankLinesBefore the desired pre-notice blank line count; use {@code 0} for no padding
   */
  public void setBlankLinesBefore (int blankLinesBefore) {

    this.blankLinesBefore = blankLinesBefore;
  }

  /**
   * Returns the number of blank lines written immediately after the notice block.
   *
   * @return the post-notice blank line count; {@code 0} means no padding
   */
  public int getBlankLinesAfter () {

    return blankLinesAfter;
  }

  /**
   * Sets the number of blank lines written immediately after the notice block.
   *
   * @param blankLinesAfter the desired post-notice blank line count; use {@code 0} for no padding
   */
  public void setBlankLinesAfter (int blankLinesAfter) {

    this.blankLinesAfter = blankLinesAfter;
  }
}

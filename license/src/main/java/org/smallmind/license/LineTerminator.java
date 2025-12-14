package org.smallmind.license;

/**
 * Resolves the correct line separator string based on a configured {@link LineEndings} setting.
 */
public class LineTerminator {

  private static final String SYSTEM_SEPARATOR = System.getProperty("line.separator");
  public LineEndings lineEndings;

  /**
   * Constructs a terminator that will supply line separators according to the given ending style.
   *
   * @param lineEndings the target line ending style to apply when writing text
   */
  public LineTerminator (LineEndings lineEndings) {

    this.lineEndings = lineEndings;
  }

  /**
   * Returns the line separator string matching the configured {@link LineEndings}.
   *
   * @return the correct line separator string for the configured style
   * @throws RuntimeException if the configured line ending value is not recognized
   */
  public String end () {

    switch (lineEndings) {
      case SYSTEM:
        return SYSTEM_SEPARATOR;
      case UNIX:
        return "\n";
      default:
        throw new RuntimeException("Unknown switch case(" + lineEndings.name() + ")");
    }
  }
}

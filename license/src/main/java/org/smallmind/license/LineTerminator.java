package org.smallmind.license;

/**
 * Supplies the correct line separator string for a configured {@link LineEndings} style.
 */
public class LineTerminator {

  private static final String SYSTEM_SEPARATOR = System.getProperty("line.separator");

  /**
   * The line-ending style that governs the separator returned by {@link #end()}.
   */
  public LineEndings lineEndings;

  /**
   * Constructs a terminator that resolves separators according to the given style.
   *
   * @param lineEndings the desired line-ending style; must not be {@code null}
   */
  public LineTerminator (LineEndings lineEndings) {

    this.lineEndings = lineEndings;
  }

  /**
   * Returns the line separator string for the configured style.
   *
   * @return {@code "\n"} for {@link LineEndings#UNIX}; the value of
   * {@code System.getProperty("line.separator")} for {@link LineEndings#SYSTEM}
   * @throws RuntimeException if the configured {@link LineEndings} constant is not handled by
   *                          the switch statement
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

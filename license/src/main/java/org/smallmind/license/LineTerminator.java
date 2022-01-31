package org.smallmind.license;

public class LineTerminator {

  private static final String SYSTEM_SEPARATOR = System.getProperty("line.separator");
  public LineEndings lineEndings;

  public LineTerminator (LineEndings lineEndings) {

    this.lineEndings = lineEndings;
  }

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

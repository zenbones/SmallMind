package org.smallmind.sleuth.maven.surefire;

import org.apache.maven.surefire.api.report.OutputReportEntry;

public class ConsoleOutputReportEntry implements OutputReportEntry {

  private final String message;
  private final boolean stdOut;
  private final boolean newLine;

  public ConsoleOutputReportEntry (String message, boolean stdOut) {

    this(message, stdOut, true);
  }

  public ConsoleOutputReportEntry (String message, boolean stdOut, boolean newLine) {

    this.message = message;
    this.stdOut = stdOut;
    this.newLine = newLine;
  }

  @Override
  public String getLog () {

    return message;
  }

  @Override
  public boolean isStdOut () {

    return stdOut;
  }

  @Override
  public boolean isNewLine () {

    return newLine;
  }
}

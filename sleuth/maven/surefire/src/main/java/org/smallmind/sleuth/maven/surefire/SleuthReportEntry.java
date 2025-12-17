/*
 * Copyright (c) 2007 through 2026 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.sleuth.maven.surefire;

import org.apache.maven.surefire.api.report.OutputReportEntry;

/**
 * Simple output entry implementation used to stream test output through Surefire.
 */
public class SleuthReportEntry implements OutputReportEntry {

  private final String message;
  private final boolean stdOut;
  private final boolean newLine;

  /**
   * Creates a report entry without forcing a trailing newline flag.
   *
   * @param message message contents
   * @param stdOut  {@code true} if the message originated on stdout, {@code false} for stderr
   */
  public SleuthReportEntry (String message, boolean stdOut) {

    this(message, stdOut, false);
  }

  /**
   * Creates a report entry with explicit newline handling.
   *
   * @param message message contents
   * @param stdOut  {@code true} if the message originated on stdout, {@code false} for stderr
   * @param newLine {@code true} if the message already ends with a newline
   */
  public SleuthReportEntry (String message, boolean stdOut, boolean newLine) {

    this.message = message;
    this.stdOut = stdOut;
    this.newLine = newLine;
  }

  /**
   * @return the full log message
   */
  @Override
  public String getLog () {

    return message;
  }

  /**
   * @return {@code true} if the log is stdout, {@code false} otherwise
   */
  @Override
  public boolean isStdOut () {

    return stdOut;
  }

  /**
   * @return {@code true} if the message already contains a trailing newline
   */
  @Override
  public boolean isNewLine () {

    return newLine;
  }
}

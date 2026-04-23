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
 * Minimal {@link OutputReportEntry} implementation used to relay individual test output
 * lines through the Surefire reporting pipeline.
 * <p>
 * Instances are created by {@link ForwardingPrintStream} on every write and passed to
 * {@link SleuthOutputReceiver}, which wraps them in a {@code TestOutputReportEntry} before
 * forwarding to the Surefire listener.
 *
 * @see ForwardingPrintStream
 * @see SleuthOutputReceiver
 */
public class SleuthReportEntry implements OutputReportEntry {

  private final String message;
  private final boolean stdOut;
  private final boolean newLine;

  /**
   * Constructs a report entry without a trailing-newline flag (defaults to {@code false}).
   *
   * @param message the output text captured from the test; must not be {@code null}
   * @param stdOut  {@code true} if the message originated from {@code System.out}; {@code false} for {@code System.err}
   */
  public SleuthReportEntry (String message, boolean stdOut) {

    this(message, stdOut, false);
  }

  /**
   * Constructs a report entry with explicit control over the trailing-newline flag.
   *
   * @param message the output text captured from the test; must not be {@code null}
   * @param stdOut  {@code true} if the message originated from {@code System.out}; {@code false} for {@code System.err}
   * @param newLine {@code true} if the message already ends with a platform line separator
   */
  public SleuthReportEntry (String message, boolean stdOut, boolean newLine) {

    this.message = message;
    this.stdOut = stdOut;
    this.newLine = newLine;
  }

  /**
   * Returns the captured output text.
   *
   * @return the log message; never {@code null}
   */
  @Override
  public String getLog () {

    return message;
  }

  /**
   * Returns whether this entry originated from the standard output stream.
   *
   * @return {@code true} for stdout output, {@code false} for stderr output
   */
  @Override
  public boolean isStdOut () {

    return stdOut;
  }

  /**
   * Returns whether the message already ends with a platform line separator.
   *
   * @return {@code true} if the message contains a trailing newline; {@code false} otherwise
   */
  @Override
  public boolean isNewLine () {

    return newLine;
  }
}

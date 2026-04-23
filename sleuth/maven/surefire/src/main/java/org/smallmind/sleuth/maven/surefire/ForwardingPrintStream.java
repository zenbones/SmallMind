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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.maven.surefire.api.report.OutputReportEntry;
import org.apache.maven.surefire.api.report.TestOutputReceiver;

/**
 * A {@link PrintStream} that intercepts all test output and forwards it to the Surefire
 * {@link TestOutputReceiver} rather than writing to the underlying stream.
 * <p>
 * An instance is installed as {@code System.out} and {@code System.err} at the start of the
 * Sleuth test run by {@link SleuthProvider}. Each write is immediately converted to a
 * {@link SleuthReportEntry} and passed to the receiver, so Surefire can correlate the output
 * with the currently active test via the {@link org.smallmind.sleuth.runner.TestIdentifier}
 * embedded in {@link SleuthOutputReceiver}.
 * <p>
 * {@link #close()} and {@link #flush()} are intentional no-ops: the receiver consumes entries
 * immediately, so buffering and lifecycle management belong to the receiver, not to this stream.
 *
 * @see SleuthOutputReceiver
 * @see SleuthReportEntry
 * @see SleuthProvider
 */
public class ForwardingPrintStream extends PrintStream {

  private static final String LINE_SEPARATOR = System.lineSeparator();

  private final TestOutputReceiver<OutputReportEntry> testOutputReceiver;
  private final boolean stdOut;

  /**
   * Constructs a forwarding stream bound to the given receiver.
   *
   * @param testOutputReceiver receiver that accepts {@link OutputReportEntry} objects for each write; must not be {@code null}
   * @param stdOut             {@code true} when this stream replaces {@code System.out}; {@code false} when replacing {@code System.err}
   */
  ForwardingPrintStream (TestOutputReceiver<OutputReportEntry> testOutputReceiver, boolean stdOut) {

    super(new ByteArrayOutputStream());

    this.testOutputReceiver = testOutputReceiver;
    this.stdOut = stdOut;
  }

  /**
   * Forwards a byte-array slice as a single test output entry.
   *
   * @param buf source buffer; must not be {@code null}
   * @param off offset of the first byte to include
   * @param len number of bytes to include
   */
  @Override
  public void write (byte[] buf, int off, int len) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(buf, off, len), stdOut));
  }

  /**
   * Forwards an entire byte array as a single test output entry.
   *
   * @param buf source buffer to forward; must not be {@code null}
   */
  @Override
  public void write (byte[] buf) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(buf), stdOut));
  }

  /**
   * Forwards a single byte as a test output entry.
   *
   * @param b the byte to forward
   */
  @Override
  public void write (int b) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(new byte[] {(byte)b}), stdOut));
  }

  /**
   * Forwards a full line, appending the platform line separator before forwarding.
   * <p>
   * A {@code null} argument is converted to the literal string {@code "null"} before the separator
   * is appended, matching the behavior of {@link PrintStream#println(String)}.
   *
   * @param s the string to print; {@code null} is treated as {@code "null"}
   */
  @Override
  public void println (String s) {

    byte[] bytes = (((s == null) ? "null" : s) + LINE_SEPARATOR).getBytes(StandardCharsets.UTF_8);

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(bytes), stdOut));
  }

  /**
   * No-op. Closing this stream does not close the underlying {@link TestOutputReceiver}
   * or interrupt test output collection.
   */
  @Override
  public void close () {

  }

  /**
   * No-op. The receiver consumes each entry immediately on write; there is no internal buffer to flush.
   */
  @Override
  public void flush () {

  }
}

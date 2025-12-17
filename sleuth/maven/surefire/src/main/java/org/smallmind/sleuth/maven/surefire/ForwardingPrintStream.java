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
 * PrintStream that forwards every write to the Surefire {@link TestOutputReceiver}.
 * <p>
 * It captures output written by tests and relays it as {@link SleuthReportEntry} instances so that
 * Surefire can associate the messages with the currently executing test and stream them to the build log.
 */
public class ForwardingPrintStream extends PrintStream {

  private static final String LINE_SEPARATOR = System.lineSeparator();

  private final TestOutputReceiver<OutputReportEntry> testOutputReceiver;
  private final boolean stdOut;

  /**
   * Constructs a forwarding stream.
   *
   * @param testOutputReceiver receiver that accepts transformed {@link OutputReportEntry} objects
   * @param stdOut             {@code true} when this stream represents stdout, {@code false} for stderr
   */
  ForwardingPrintStream (TestOutputReceiver<OutputReportEntry> testOutputReceiver, boolean stdOut) {

    super(new ByteArrayOutputStream());

    this.testOutputReceiver = testOutputReceiver;
    this.stdOut = stdOut;
  }

  /**
   * Forwards a byte buffer slice as a test output entry.
   *
   * @param buf source buffer
   * @param off offset of the first byte to write
   * @param len number of bytes to write
   */
  @Override
  public void write (byte[] buf, int off, int len) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(buf, off, len), stdOut));
  }

  /**
   * Forwards an entire byte buffer as a test output entry.
   *
   * @param buf source buffer to forward
   */
  @Override
  public void write (byte[] buf) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(buf), stdOut));
  }

  /**
   * Forwards a single byte as a test output entry.
   *
   * @param b byte to forward
   */
  @Override
  public void write (int b) {

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(new byte[] {(byte)b}), stdOut));
  }

  /**
   * Forwards a full line, ensuring a platform line separator is appended.
   *
   * @param s string value; {@code null} is converted to the literal {@code "null"}
   */
  @Override
  public void println (String s) {

    byte[] bytes = (((s == null) ? "null" : s) + LINE_SEPARATOR).getBytes(StandardCharsets.UTF_8);

    testOutputReceiver.writeTestOutput(new SleuthReportEntry(new String(bytes), stdOut));
  }

  /**
   * No-op to avoid closing the underlying receiver-managed streams.
   */
  @Override
  public void close () {

  }

  /**
   * No-op to avoid flushing the wrapped {@link ByteArrayOutputStream}; the receiver consumes output immediately.
   */
  @Override
  public void flush () {

  }
}

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
package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link OutputStream} that multiplexes every write operation to a fixed array of delegate streams simultaneously.
 */
public class MultiOutputStream extends OutputStream {

  private final OutputStream[] streams;

  /**
   * Constructs a multi-output stream that writes to all supplied delegates.
   *
   * @param streams the delegate output streams to receive each write
   */
  public MultiOutputStream (OutputStream[] streams) {

    this.streams = streams;
  }

  /**
   * Writes the low eight bits of {@code b} to every delegate stream.
   *
   * @param b the byte value to write
   * @throws IOException if any delegate stream throws
   */
  public void write (int b)
    throws IOException {

    for (OutputStream stream : streams) {
      stream.write(new byte[] {(byte)b});
    }
  }

  /**
   * Writes all bytes from {@code buffer} to every delegate stream.
   *
   * @param buffer the data to write
   * @throws IOException if any delegate stream throws
   */
  public void write (byte[] buffer)
    throws IOException {

    for (OutputStream stream : streams) {
      stream.write(buffer, 0, buffer.length);
    }
  }

  /**
   * Writes {@code len} bytes from {@code buffer} starting at {@code off} to every delegate stream.
   *
   * @param buffer source data array
   * @param off    starting index in {@code buffer}
   * @param len    number of bytes to write
   * @throws IOException if any delegate stream throws
   */
  public void write (byte[] buffer, int off, int len)
    throws IOException {

    for (OutputStream stream : streams) {
      stream.write(buffer, off, len);
    }
  }
}

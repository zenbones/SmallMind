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
package org.smallmind.bayeux.oumuamua.server.spi;

import java.io.Writer;

/**
 * In-memory {@link Writer} that accumulates all written characters into a caller-supplied
 * {@link StringBuilder}; used to serialize Bayeux packets without intermediate I/O overhead.
 */
public class PacketWriter extends Writer {

  private final StringBuilder builder;

  /**
   * Wraps the given builder so all subsequent writes are appended to it.
   *
   * @param builder destination buffer that will receive all written data
   */
  public PacketWriter (StringBuilder builder) {

    this.builder = builder;
  }

  /**
   * Appends a single character to the buffer.
   *
   * @param c character value to append, cast to {@code char}
   */
  public void write (int c) {

    builder.append((char)c);
  }

  /**
   * Appends a range of characters from a char array to the buffer.
   *
   * @param cbuf source character array
   * @param off  index of the first character to write
   * @param len  number of characters to write
   * @throws NullPointerException      if {@code cbuf} is null
   * @throws IndexOutOfBoundsException if {@code off} or {@code len} are out of range
   */
  public void write (char cbuf[], int off, int len) {

    if (cbuf == null) {
      throw new NullPointerException();
    } else {
      if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
      } else if (len > 0) {
        builder.append(cbuf, off, len);
      }
    }
  }

  /**
   * Appends an entire string to the buffer.
   *
   * @param str the string to append
   * @throws NullPointerException if {@code str} is null
   */
  public void write (String str) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str);
    }
  }

  /**
   * Appends a substring to the buffer.
   *
   * @param str source string
   * @param off index of the first character within {@code str} to write
   * @param len number of characters to write
   * @throws NullPointerException if {@code str} is null
   */
  public void write (String str, int off, int len) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str, off, off + len);
    }
  }

  /**
   * No-op; all data resides in-memory and requires no flushing.
   */
  @Override
  public void flush () {

  }

  /**
   * No-op; no external resources are held by this writer.
   */
  @Override
  public void close () {

  }

  /**
   * Returns the accumulated content as a string.
   *
   * @return current contents of the underlying {@link StringBuilder}
   */
  public String toString () {

    return builder.toString();
  }
}

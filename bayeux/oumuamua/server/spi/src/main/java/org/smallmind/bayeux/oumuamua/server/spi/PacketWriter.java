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
 * Simple {@link Writer} that appends all output to a provided {@link StringBuilder}.
 */
public class PacketWriter extends Writer {

  private final StringBuilder builder;

  /**
   * Creates a writer that appends to the supplied builder.
   *
   * @param builder destination builder
   */
  public PacketWriter (StringBuilder builder) {

    this.builder = builder;
  }

  /**
   * Writes a single character to the builder.
   *
   * @param c character to write
   */
  public void write (int c) {

    builder.append((char)c);
  }

  /**
   * Writes a range of characters from an array.
   *
   * @param cbuf source buffer
   * @param off  offset in the buffer
   * @param len  number of characters to write
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
   * Writes a complete string.
   *
   * @param str string to write
   */
  public void write (String str) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str);
    }
  }

  /**
   * Writes a substring.
   *
   * @param str source string
   * @param off starting offset
   * @param len number of characters
   */
  public void write (String str, int off, int len) {

    if (str == null) {
      throw new NullPointerException();
    } else {
      builder.append(str, off, off + len);
    }
  }

  /**
   * No-op flush because data is kept in memory.
   */
  @Override
  public void flush () {

  }

  /**
   * No-op close because there are no external resources.
   */
  @Override
  public void close () {

  }

  /**
   * @return string representation of buffered data
   */
  public String toString () {

    return builder.toString();
  }
}

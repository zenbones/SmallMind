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
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Wraps a {@link ByteBuffer} as an {@link InputStream}, reading sequentially from the buffer's current position.
 */
public class ByteBufferInputStream extends InputStream {

  private final ByteBuffer buffer;

  /**
   * Constructs a stream backed by the given buffer.
   *
   * @param buffer the buffer to read from; data is consumed from its current position up to its limit
   */
  public ByteBufferInputStream (ByteBuffer buffer) {

    this.buffer = buffer;
  }

  /**
   * Reads the next byte from the buffer as an unsigned value, or {@code -1} when exhausted.
   *
   * @return the next byte in the range 0–255, or {@code -1} if no bytes remain
   * @throws IOException not thrown; declared for interface compatibility
   */
  @Override
  public int read ()
    throws IOException {

    return (buffer.position() < buffer.limit()) ? buffer.get() & 0xFF : -1;
  }

  /**
   * Reads up to {@code len} bytes from the buffer into the destination array.
   *
   * @param b   destination array
   * @param off starting index in {@code b}
   * @param len maximum number of bytes to transfer
   * @return number of bytes actually read, or {@code -1} if no bytes remain in the buffer
   * @throws IOException               not thrown; declared for interface compatibility
   * @throws IndexOutOfBoundsException if the offset or length are out of range for {@code b}
   */
  @Override
  public int read (byte[] b, int off, int len)
    throws IOException {

    Objects.checkFromIndexSize(off, len, b.length);

    if (buffer.remaining() == 0) {
      return -1;
    } else {

      int bytesRead;

      buffer.get(b, off, bytesRead = Math.min(len, buffer.limit() - buffer.position()));

      return bytesRead;
    }
  }
}

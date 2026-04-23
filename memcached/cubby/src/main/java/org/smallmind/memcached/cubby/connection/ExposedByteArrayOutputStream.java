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
package org.smallmind.memcached.cubby.connection;

import java.io.ByteArrayOutputStream;

/**
 * A {@link ByteArrayOutputStream} subclass that exposes its internal backing buffer directly,
 * avoiding the defensive copy that {@link ByteArrayOutputStream#toByteArray()} would otherwise
 * produce.
 *
 * <p>This class is used by {@link ResponseReader} to accumulate partial read data between NIO
 * select cycles. Callers that obtain the buffer via {@link #getBuffer()} must respect the
 * stream's {@code count} field (inherited from {@link ByteArrayOutputStream}) to determine
 * how many bytes are valid, and must not modify the array contents in a way that would
 * corrupt the stream's state.</p>
 */
public class ExposedByteArrayOutputStream extends ByteArrayOutputStream {

  /**
   * Constructs the stream with the given initial buffer capacity.
   *
   * @param size the initial size of the internal buffer in bytes; the buffer grows
   *             automatically if more bytes are written than the initial capacity allows
   */
  public ExposedByteArrayOutputStream (int size) {

    super(size);
  }

  /**
   * Returns a direct reference to the internal byte array backing this stream.
   *
   * <p>Only bytes in the range {@code [0, count)} contain valid data, where {@code count}
   * is the protected field inherited from {@link ByteArrayOutputStream}. The reference is
   * stable as long as no write operation causes the buffer to be reallocated.</p>
   *
   * @return the internal backing buffer; may be larger than the number of bytes written
   */
  public byte[] getBuffer () {

    return buf;
  }
}

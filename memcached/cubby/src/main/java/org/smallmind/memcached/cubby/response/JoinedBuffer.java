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
package org.smallmind.memcached.cubby.response;

import java.nio.ByteBuffer;
import org.smallmind.memcached.cubby.connection.ExposedByteArrayOutputStream;
import org.smallmind.nutsnbolts.lang.FormattedIllegalArgumentException;

/**
 * Presents a unified, position-tracked view over two consecutive byte sources used during
 * response parsing: an accumulation buffer holding bytes from previous reads, and a
 * newly received {@link ByteBuffer}.
 *
 * <p>The Cubby connection layer may receive partial responses across multiple NIO read
 * operations. When a complete line has not yet arrived, the bytes read so far are stored in
 * an {@code ExposedByteArrayOutputStream}. On the next read the new bytes are combined with
 * the accumulated bytes via a {@code JoinedBuffer}, allowing the parser to treat both sources
 * as a single contiguous stream without copying data.</p>
 *
 * <p>The class maintains its own logical {@code position} counter and keeps the underlying
 * buffer positions in sync. It supports a single {@link #mark()}/{@link #reset()} pair
 * analogous to {@link ByteBuffer}, as well as absolute-index access via {@link #get(int)} and
 * look-ahead via {@link #peek(int)}.</p>
 */
public class JoinedBuffer {

  private static final byte[] NO_BYTES = new byte[0];

  private final ByteBuffer readBuffer;
  private final ByteBuffer accumulatingBuffer;
  private final int limit;
  private int position = 0;
  private int mark = -1;

  /**
   * Constructs a joined view over the accumulated stream and the current read buffer.
   *
   * <p>The accumulating stream is wrapped as a read-only {@link ByteBuffer} covering exactly
   * the bytes written so far ({@code 0} to {@code accumulatingStream.size()}). The logical
   * limit is the sum of that count and {@code readBuffer.limit()}.</p>
   *
   * @param accumulatingStream the stream holding bytes received in previous partial reads
   * @param readBuffer         the buffer containing the bytes from the most recent NIO read
   */
  public JoinedBuffer (ExposedByteArrayOutputStream accumulatingStream, ByteBuffer readBuffer) {

    this.readBuffer = readBuffer;

    accumulatingBuffer = ByteBuffer.wrap(accumulatingStream.getBuffer(), 0, accumulatingStream.size());

    limit = accumulatingStream.size() + readBuffer.limit();
  }

  /**
   * Returns the byte at a given offset from the current position without advancing the position.
   *
   * @param index the zero-based offset from the current position
   * @return the byte at {@code position() + index}
   */
  public byte peek (int index) {

    return get(position() + index);
  }

  /**
   * Reads and returns the next byte, advancing the logical position by one.
   *
   * <p>Bytes are drawn from the accumulating buffer first; once it is exhausted subsequent
   * bytes come from the read buffer.</p>
   *
   * @return the next byte in the joined stream
   */
  public byte get () {

    if (position++ < accumulatingBuffer.limit()) {
      return accumulatingBuffer.get();
    } else {
      return readBuffer.get();
    }
  }

  /**
   * Returns the byte at the given absolute index without altering the current position.
   *
   * @param index the absolute zero-based index within the joined stream
   * @return the byte at that index
   */
  public byte get (int index) {

    if (index < accumulatingBuffer.limit()) {
      return accumulatingBuffer.get(index);
    } else {
      return readBuffer.get(index - accumulatingBuffer.limit());
    }
  }

  /**
   * Reads exactly {@code buffer.length} bytes into the supplied array, advancing the position
   * by the same amount.
   *
   * <p>Bytes are taken from the accumulating buffer first; any remaining bytes are taken from
   * the read buffer.</p>
   *
   * @param buffer the destination array; its length determines how many bytes are read
   * @return the same {@code buffer} instance, now filled with the read data
   */
  public byte[] get (byte[] buffer) {

    int bytesRead = 0;

    if (position < accumulatingBuffer.limit()) {
      accumulatingBuffer.get(buffer, 0, bytesRead = Math.min(accumulatingBuffer.remaining(), buffer.length));
    }
    if (bytesRead < buffer.length) {
      readBuffer.get(buffer, bytesRead, buffer.length - bytesRead);
    }

    position += buffer.length;

    return buffer;
  }

  /**
   * Returns the total number of bytes available across both underlying buffers.
   *
   * @return the combined limit of the accumulating buffer and the read buffer
   */
  public int limit () {

    return limit;
  }

  /**
   * Returns the current logical read position within the joined stream.
   *
   * @return the current position, measured from the start of the accumulating buffer
   */
  public int position () {

    return position;
  }

  /**
   * Advances the current position by the given delta and returns the new position.
   *
   * @param delta the number of positions to move forward
   * @return the new logical position after the increment
   */
  public int incPosition (int delta) {

    position(position + delta);

    return position;
  }

  /**
   * Sets the logical position to an absolute value and synchronises the positions of both
   * underlying buffers accordingly.
   *
   * <p>If the new position falls within the accumulating buffer, that buffer is positioned
   * directly and the read buffer is rewound to zero. If the new position lies in the read
   * buffer, the accumulating buffer is advanced to its limit and the read buffer is positioned
   * at the offset relative to where the read buffer begins.</p>
   *
   * <p>Any previously recorded mark that is greater than the new position is invalidated.</p>
   *
   * @param position the target absolute position
   */
  public void position (int position) {

    if (position < 0) {
      throw new FormattedIllegalArgumentException("Attempt to set position < 0");
    } else if (position > accumulatingBuffer.limit() + readBuffer.limit()) {

      throw new FormattedIllegalArgumentException("Attempt to set position > %d", accumulatingBuffer.limit() + readBuffer.limit());
    } else {

      this.position = position;

      if (position < accumulatingBuffer.limit()) {
        accumulatingBuffer.position(position);
        readBuffer.position(0);
      } else {
        accumulatingBuffer.position(accumulatingBuffer.limit());
        readBuffer.position(position - accumulatingBuffer.limit());
      }

      if (mark > position) {
        mark = -1;
      }
    }
  }

  /**
   * Returns the number of bytes remaining between the current position and the limit.
   *
   * @return {@code limit() - position()}
   */
  public int remaining () {

    return limit - position;
  }

  /**
   * Records the current position so that a later call to {@link #reset()} can return to it.
   *
   * <p>The mark is recorded on whichever underlying buffer owns the current position.</p>
   */
  public void mark () {

    if (position < accumulatingBuffer.limit()) {
      accumulatingBuffer.mark();
    } else {
      readBuffer.mark();
    }

    mark = position;
  }

  /**
   * Resets the logical position to the value recorded by the most recent call to {@link #mark()}.
   *
   * <p>If no mark has been set this method is a no-op. Both underlying buffer positions are
   * restored to be consistent with the marked logical position.</p>
   */
  public void reset () {

    if (mark >= 0) {
      if (mark < accumulatingBuffer.limit()) {
        accumulatingBuffer.reset();
        readBuffer.position(0);
      } else {
        accumulatingBuffer.position(accumulatingBuffer.limit());
        readBuffer.reset();
      }

      position = mark;
    }
  }
}

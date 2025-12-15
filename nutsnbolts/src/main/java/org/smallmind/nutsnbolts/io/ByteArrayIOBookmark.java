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

import java.util.ArrayList;

/**
 * Mutable bookmark used by {@link ByteArrayIOStream} to track position across segmented byte arrays.
 * Supports arithmetic on positions and conversion to/from absolute offsets.
 */
public class ByteArrayIOBookmark {

  private final int allocation;
  private int segmentIndex;
  private int byteIndex;

  /**
   * Creates a bookmark at position 0 for the given segment allocation size.
   *
   * @param allocation number of bytes per segment
   */
  public ByteArrayIOBookmark (int allocation) {

    this.allocation = allocation;
  }

  /**
   * Creates a bookmark at a specific absolute position.
   *
   * @param allocation number of bytes per segment
   * @param position   absolute offset into the segmented buffer
   */
  public ByteArrayIOBookmark (int allocation, long position) {

    this(allocation);

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  /**
   * Copy constructor.
   *
   * @param bookmark bookmark to clone
   */
  public ByteArrayIOBookmark (ByteArrayIOBookmark bookmark) {

    this(bookmark.getAllocation());

    segmentIndex = bookmark.segmentIndex();
    byteIndex = bookmark.byteIndex();
  }

  /**
   * @return allocation (segment size) in bytes
   */
  public int getAllocation () {

    return allocation;
  }

  /**
   * @return current segment index
   */
  public int segmentIndex () {

    return segmentIndex;
  }

  /**
   * @return offset within the current segment
   */
  public int byteIndex () {

    return byteIndex;
  }

  /**
   * @return absolute position computed from segment and byte offsets
   */
  public long position () {

    return (segmentIndex * ((long)allocation)) + byteIndex;
  }

  /**
   * Moves the bookmark to an absolute position.
   *
   * @param position absolute offset into the segmented buffer
   */
  public void position (long position) {

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  /**
   * Resets position to the start of the buffer.
   */
  public void rewind () {

    segmentIndex = 0;
    byteIndex = 0;
  }

  /**
   * Resets this bookmark to another bookmark's position, if provided.
   *
   * @param bookmark source bookmark (may be {@code null})
   * @return this bookmark for chaining
   */
  public ByteArrayIOBookmark reset (ByteArrayIOBookmark bookmark) {

    if (bookmark != null) {
      segmentIndex = bookmark.segmentIndex();
      byteIndex = bookmark.byteIndex();
    }

    return this;
  }

  /**
   * Increments the bookmark by one byte, respecting the upper limit bookmark.
   *
   * @param limitBookmark bookmark representing end-of-stream
   * @param segmentList   backing segment list for bounds checks
   * @return this bookmark after increment
   * @throws IllegalStateException if increment moves past end-of-stream
   */
  public ByteArrayIOBookmark inc (ByteArrayIOBookmark limitBookmark, ArrayList<byte[]> segmentList) {

    if (segmentIndex < segmentList.size()) {
      if (++byteIndex == allocation) {
        byteIndex = 0;
        segmentIndex++;
      }
    } else if ((this == limitBookmark) || (byteIndex <= limitBookmark.byteIndex())) {
      byteIndex++;
    } else {
      throw new IllegalStateException("End of stream");
    }

    return this;
  }

  /**
   * Advances the bookmark by {@code n} bytes, validating against the limit.
   *
   * @param limitBookmark bookmark representing end-of-stream
   * @param n             number of bytes to skip (non-negative)
   * @return this bookmark after skipping
   * @throws IllegalArgumentException if the resulting position exceeds the limit
   */
  public ByteArrayIOBookmark skip (ByteArrayIOBookmark limitBookmark, long n) {

    long futurePosition = position() + n;

    if (futurePosition > limitBookmark.position()) {
      throw new IllegalArgumentException("Offset not within bounds");
    } else {

      segmentIndex = (int)(futurePosition / allocation);
      byteIndex = (int)(futurePosition % allocation);

      return this;
    }
  }

  /**
   * Produces a new bookmark offset by {@code delta}, enforcing bounds between zero and the limit.
   *
   * @param limitBookmark bookmark representing end-of-stream
   * @param delta         signed offset
   * @return new bookmark at the adjusted position
   * @throws IllegalArgumentException if the result is out of bounds
   */
  public ByteArrayIOBookmark offset (ByteArrayIOBookmark limitBookmark, long delta) {

    long futurePosition = position() + delta;

    if ((futurePosition < 0) || (futurePosition > limitBookmark.position())) {
      throw new IllegalArgumentException("Offset not within bounds");
    } else {

      return new ByteArrayIOBookmark(allocation, futurePosition);
    }
  }

  /**
   * Compares two bookmarks by segment and byte index.
   *
   * @param obj other object
   * @return {@code true} if both refer to the same position
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ByteArrayIOBookmark) && (((ByteArrayIOBookmark)obj).segmentIndex() == segmentIndex) && (((ByteArrayIOBookmark)obj).byteIndex() == byteIndex);
  }
}

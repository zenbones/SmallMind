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
 * Mutable cursor into a segmented byte-array buffer, tracking a segment index and byte offset
 * and supporting positional arithmetic used by {@link ByteArrayIOStream}.
 */
public class ByteArrayIOBookmark {

  private final int allocation;
  private int segmentIndex;
  private int byteIndex;

  /**
   * Creates a bookmark positioned at offset zero for the given segment size.
   *
   * @param allocation number of bytes per segment
   */
  public ByteArrayIOBookmark (int allocation) {

    this.allocation = allocation;
  }

  /**
   * Creates a bookmark at the given absolute byte position within the segmented buffer.
   *
   * @param allocation number of bytes per segment
   * @param position   absolute offset to initialize the bookmark at
   */
  public ByteArrayIOBookmark (int allocation, long position) {

    this(allocation);

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  /**
   * Creates a bookmark whose initial position matches an existing bookmark.
   *
   * @param bookmark the bookmark to copy the position from
   */
  public ByteArrayIOBookmark (ByteArrayIOBookmark bookmark) {

    this(bookmark.getAllocation());

    segmentIndex = bookmark.segmentIndex();
    byteIndex = bookmark.byteIndex();
  }

  /**
   * Returns the segment size that this bookmark was constructed with.
   *
   * @return number of bytes per segment
   */
  public int getAllocation () {

    return allocation;
  }

  /**
   * Returns the zero-based index of the segment the bookmark currently points into.
   *
   * @return current segment index
   */
  public int segmentIndex () {

    return segmentIndex;
  }

  /**
   * Returns the byte offset within the current segment.
   *
   * @return byte offset within the current segment (0 inclusive to allocation exclusive)
   */
  public int byteIndex () {

    return byteIndex;
  }

  /**
   * Computes the absolute byte position represented by this bookmark.
   *
   * @return {@code segmentIndex * allocation + byteIndex}
   */
  public long position () {

    return (segmentIndex * ((long)allocation)) + byteIndex;
  }

  /**
   * Moves this bookmark to the given absolute byte position.
   *
   * @param position new absolute position within the segmented buffer
   */
  public void position (long position) {

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  /**
   * Sets this bookmark back to position zero (segment 0, byte 0).
   */
  public void rewind () {

    segmentIndex = 0;
    byteIndex = 0;
  }

  /**
   * Copies the position from another bookmark into this one, if the source is non-null.
   *
   * @param bookmark source bookmark whose position is copied; a {@code null} value is a no-op
   * @return this bookmark for method chaining
   */
  public ByteArrayIOBookmark reset (ByteArrayIOBookmark bookmark) {

    if (bookmark != null) {
      segmentIndex = bookmark.segmentIndex();
      byteIndex = bookmark.byteIndex();
    }

    return this;
  }

  /**
   * Advances this bookmark forward by one byte, wrapping to the next segment when the current one is exhausted.
   *
   * @param limitBookmark bookmark representing the end of written data
   * @param segmentList   the list of backing segments used for bounds checking
   * @return this bookmark for method chaining
   * @throws IllegalStateException if advancing would move beyond the limit
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
   * Advances this bookmark forward by {@code n} bytes in a single operation, validating against the limit.
   *
   * @param limitBookmark bookmark representing the end of written data
   * @param n             number of bytes to advance (must be non-negative)
   * @return this bookmark for method chaining
   * @throws IllegalArgumentException if the new position would exceed the limit
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
   * Creates a new bookmark at a position relative to this one, enforcing bounds between zero and the limit.
   *
   * @param limitBookmark bookmark representing the end of written data
   * @param delta         signed offset to apply to the current position
   * @return a new bookmark at the resulting position
   * @throws IllegalArgumentException if the result would be negative or exceed the limit
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
   * Returns {@code true} when {@code obj} is a {@link ByteArrayIOBookmark} at the same segment index and byte offset.
   *
   * @param obj the object to compare with
   * @return {@code true} if both bookmarks represent the same logical position
   */
  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ByteArrayIOBookmark) && (((ByteArrayIOBookmark)obj).segmentIndex() == segmentIndex) && (((ByteArrayIOBookmark)obj).byteIndex() == byteIndex);
  }
}

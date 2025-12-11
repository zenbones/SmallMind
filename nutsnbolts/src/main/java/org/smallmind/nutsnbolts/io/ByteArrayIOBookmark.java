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

public class ByteArrayIOBookmark {

  private final int allocation;
  private int segmentIndex;
  private int byteIndex;

  public ByteArrayIOBookmark (int allocation) {

    this.allocation = allocation;
  }

  public ByteArrayIOBookmark (int allocation, long position) {

    this(allocation);

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  public ByteArrayIOBookmark (ByteArrayIOBookmark bookmark) {

    this(bookmark.getAllocation());

    segmentIndex = bookmark.segmentIndex();
    byteIndex = bookmark.byteIndex();
  }

  public int getAllocation () {

    return allocation;
  }

  public int segmentIndex () {

    return segmentIndex;
  }

  public int byteIndex () {

    return byteIndex;
  }

  public long position () {

    return (segmentIndex * ((long)allocation)) + byteIndex;
  }

  public void position (long position) {

    segmentIndex = (int)(position / allocation);
    byteIndex = (int)(position % allocation);
  }

  public void rewind () {

    segmentIndex = 0;
    byteIndex = 0;
  }

  public ByteArrayIOBookmark reset (ByteArrayIOBookmark bookmark) {

    if (bookmark != null) {
      segmentIndex = bookmark.segmentIndex();
      byteIndex = bookmark.byteIndex();
    }

    return this;
  }

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

  public ByteArrayIOBookmark offset (ByteArrayIOBookmark limitBookmark, long delta) {

    long futurePosition = position() + delta;

    if ((futurePosition < 0) || (futurePosition > limitBookmark.position())) {
      throw new IllegalArgumentException("Offset not within bounds");
    } else {

      return new ByteArrayIOBookmark(allocation, futurePosition);
    }
  }

  @Override
  public boolean equals (Object obj) {

    return (obj instanceof ByteArrayIOBookmark) && (((ByteArrayIOBookmark)obj).segmentIndex() == segmentIndex) && (((ByteArrayIOBookmark)obj).byteIndex() == byteIndex);
  }
}

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

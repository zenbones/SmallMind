package org.smallmind.nutsnbolts.io;

import java.io.IOException;
import java.util.ArrayList;

public class ByteArrayIOBuffer {

  private final ArrayList<byte[]> segmentList = new ArrayList<>();
  private final ByteArrayIOBookmark limitBookmark;
  private final int allocation;

  public ByteArrayIOBuffer (int allocation) {

    this.allocation = allocation;

    limitBookmark = new ByteArrayIOBookmark(allocation);
  }

  public ByteArrayIOBuffer (ByteArrayIOBuffer segmentBuffer) {

    allocation = segmentBuffer.getAllocation();
    limitBookmark = new ByteArrayIOBookmark(segmentBuffer.getLimitBookmark());

    for (byte[] segment : segmentBuffer.getSegmentList()) {

      byte[] copyOfSegment = new byte[segment.length];

      System.arraycopy(segment, 0, copyOfSegment, 0, segment.length);
      segmentList.add(copyOfSegment);
    }
  }

  public int getAllocation () {

    return allocation;
  }

  public ArrayList<byte[]> getSegmentList () {

    return segmentList;
  }

  public ByteArrayIOBookmark getLimitBookmark () {

    return limitBookmark;
  }

  public void clear ()
    throws IOException {

    segmentList.clear();
    limitBookmark.rewind();
  }
}

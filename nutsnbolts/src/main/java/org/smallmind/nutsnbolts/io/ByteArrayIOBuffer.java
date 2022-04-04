package org.smallmind.nutsnbolts.io;

import java.util.ArrayList;

public class ByteArrayIOBuffer {

  private final ArrayList<byte[]> segmentList = new ArrayList<>();
  private final ByteArrayIOBookmark limitBookmark;
  private final int allocation;

  public ByteArrayIOBuffer (int allocation) {

    this.allocation = allocation;

    limitBookmark = new ByteArrayIOBookmark(allocation);
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
}

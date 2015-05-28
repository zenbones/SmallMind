package org.smallmind.throng.wire.jms.hornetq.spring;

public class HornetQAddressConfiguration {

  private long maxSizeBytes;
  private long pageSizeBytes;

  public long getMaxSizeBytes () {

    return maxSizeBytes;
  }

  public void setMaxSizeBytes (long maxSizeBytes) {

    this.maxSizeBytes = maxSizeBytes;
  }

  public long getPageSizeBytes () {

    return pageSizeBytes;
  }

  public void setPageSizeBytes (long pageSizeBytes) {

    this.pageSizeBytes = pageSizeBytes;
  }
}
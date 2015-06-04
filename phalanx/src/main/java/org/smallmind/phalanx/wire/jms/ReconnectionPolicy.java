package org.smallmind.phalanx.wire.jms;

public class ReconnectionPolicy {

  private long reconnectionDelayMilliseconds = 500;
  private int reconnectionAttempts = -1;

  public int getReconnectionAttempts () {

    return reconnectionAttempts;
  }

  public void setReconnectionAttempts (int reconnectionAttempts) {

    this.reconnectionAttempts = reconnectionAttempts;
  }

  public long getReconnectionDelayMilliseconds () {

    return reconnectionDelayMilliseconds;
  }

  public void setReconnectionDelayMilliseconds (long reconnectionDelayMilliseconds) {

    this.reconnectionDelayMilliseconds = reconnectionDelayMilliseconds;
  }
}
package org.smallmind.quorum.transport.message;

public class ReconnectionPolicy {

  private long reconnectionDelayMilliseconds = 300;
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

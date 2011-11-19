package org.smallmind.quorum.pool2;

public class ConnectionPoolConfig {

  private boolean testOnConnect = false;
  private boolean testOnAcquire = false;
  private boolean reportLeaseTimeNanos = true;
  private boolean existentiallyAware = false;
  private long connectionTimeoutMillis = 0;
  private long acquireWaitTimeMillis = 0;
  private int initialPoolSize = 0;
  private int minPoolSize = 0;
  private int maxPoolSize = 10;
  private int maxLeaseTimeSeconds = 0;
  private int maxIdleTimeSeconds = 0;
  private int unreturnedConnectionTimeoutSeconds = 0;

  public synchronized boolean isTestOnConnect () {

    return testOnConnect;
  }

  public synchronized void setTestOnConnect (boolean testOnConnect) {

    this.testOnConnect = testOnConnect;
  }

  public synchronized boolean isTestOnAcquire () {

    return testOnAcquire;
  }

  public synchronized void setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire = testOnAcquire;
  }

  public synchronized boolean isReportLeaseTimeNanos () {

    return reportLeaseTimeNanos;
  }

  public synchronized void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    this.reportLeaseTimeNanos = reportLeaseTimeNanos;
  }

  public synchronized boolean isExistentiallyAware () {

    return existentiallyAware;
  }

  public synchronized void setExistentiallyAware (boolean existentiallyAware) {

    this.existentiallyAware = existentiallyAware;
  }

  public synchronized long getConnectionTimeoutMillis () {

    return connectionTimeoutMillis;
  }

  public synchronized void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    if (connectionTimeoutMillis < 0) {
      throw new IllegalArgumentException("Connection timeout must be >= 0");
    }

    this.connectionTimeoutMillis = connectionTimeoutMillis;
  }

  public synchronized int getInitialPoolSize () {

    return initialPoolSize;
  }

  public synchronized void setInitialPoolSize (int initialPoolSize) {

    if (initialPoolSize < 0) {
      throw new IllegalArgumentException("Initial pool size must be >= 0");
    }

    this.initialPoolSize = initialPoolSize;
  }

  public synchronized int getMinPoolSize () {

    return minPoolSize;
  }

  public synchronized void setMinPoolSize (int minPoolSize) {

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size must be >= 0");
    }

    this.minPoolSize = minPoolSize;
  }

  public synchronized int getMaxPoolSize () {

    return maxPoolSize;
  }

  public synchronized void setMaxPoolSize (int maxPoolSize) {

    if (maxPoolSize < 0) {
      throw new IllegalArgumentException("Maximum pool size must be >= 0");
    }

    this.maxPoolSize = maxPoolSize;
  }

  public synchronized long getAcquireWaitTimeMillis () {

    return acquireWaitTimeMillis;
  }

  public synchronized void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    if (acquireWaitTimeMillis < 0) {
      throw new IllegalArgumentException("Acquire wait time must be >= 0");
    }

    this.acquireWaitTimeMillis = acquireWaitTimeMillis;
  }

  public synchronized int getMaxLeaseTimeSeconds () {

    return maxLeaseTimeSeconds;
  }

  public synchronized void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    if (maxLeaseTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum lease time must be >= 0");
    }

    this.maxLeaseTimeSeconds = maxLeaseTimeSeconds;
  }

  public synchronized int getMaxIdleTimeSeconds () {

    return maxIdleTimeSeconds;
  }

  public synchronized void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    if (maxIdleTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum idle time must be >= 0");
    }

    this.maxIdleTimeSeconds = maxIdleTimeSeconds;
  }

  public synchronized int getUnreturnedConnectionTimeoutSeconds () {

    return unreturnedConnectionTimeoutSeconds;
  }

  public synchronized void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

    if (unreturnedConnectionTimeoutSeconds < 0) {
      throw new IllegalArgumentException("Unreturned connection timeout must be >= 0");
    }

    this.unreturnedConnectionTimeoutSeconds = unreturnedConnectionTimeoutSeconds;
  }
}

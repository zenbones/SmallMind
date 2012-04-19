/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.pool.connection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionPoolConfig {

  private final AtomicBoolean testOnConnect = new AtomicBoolean(false);
  private final AtomicBoolean testOnAcquire = new AtomicBoolean(false);
  private final AtomicBoolean reportLeaseTimeNanos = new AtomicBoolean(false);
  private final AtomicBoolean existentiallyAware = new AtomicBoolean(false);
  private final AtomicLong connectionTimeoutMillis = new AtomicLong(0);
  private final AtomicLong acquireWaitTimeMillis = new AtomicLong(0);
  private final AtomicInteger initialPoolSize = new AtomicInteger(0);
  private final AtomicInteger minPoolSize = new AtomicInteger(0);
  private final AtomicInteger maxPoolSize = new AtomicInteger(10);
  private final AtomicInteger maxLeaseTimeSeconds = new AtomicInteger(0);
  private final AtomicInteger maxIdleTimeSeconds = new AtomicInteger(0);
  private final AtomicInteger unreturnedConnectionTimeoutSeconds = new AtomicInteger(0);

  public boolean requiresDeconstruction () {

    return (getMaxLeaseTimeSeconds() > 0) || (getMaxIdleTimeSeconds() > 0) || (getUnreturnedConnectionTimeoutSeconds() > 0);
  }

  public boolean isTestOnConnect () {

    return testOnConnect.get();
  }

  public ConnectionPoolConfig setTestOnConnect (boolean testOnConnect) {

    this.testOnConnect.set(testOnConnect);

    return this;
  }

  public boolean isTestOnAcquire () {

    return testOnAcquire.get();
  }

  public ConnectionPoolConfig setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire.set(testOnAcquire);

    return this;
  }

  public boolean isReportLeaseTimeNanos () {

    return reportLeaseTimeNanos.get();
  }

  public ConnectionPoolConfig setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    this.reportLeaseTimeNanos.set(reportLeaseTimeNanos);

    return this;
  }

  public boolean isExistentiallyAware () {

    return existentiallyAware.get();
  }

  public ConnectionPoolConfig setExistentiallyAware (boolean existentiallyAware) {

    this.existentiallyAware.set(existentiallyAware);

    return this;
  }

  public long getConnectionTimeoutMillis () {

    return connectionTimeoutMillis.get();
  }

  public ConnectionPoolConfig setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    if (connectionTimeoutMillis < 0) {
      throw new IllegalArgumentException("Connection timeout must be >= 0");
    }

    this.connectionTimeoutMillis.set(connectionTimeoutMillis);

    return this;
  }

  public int getInitialPoolSize () {

    return initialPoolSize.get();
  }

  public ConnectionPoolConfig setInitialPoolSize (int initialPoolSize) {

    if (initialPoolSize < 0) {
      throw new IllegalArgumentException("Initial pool size must be >= 0");
    }

    this.initialPoolSize.set(initialPoolSize);

    return this;
  }

  public int getMinPoolSize () {

    return minPoolSize.get();
  }

  public ConnectionPoolConfig setMinPoolSize (int minPoolSize) {

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size must be >= 0");
    }

    this.minPoolSize.set(minPoolSize);

    return this;
  }

  public int getMaxPoolSize () {

    return maxPoolSize.get();
  }

  public ConnectionPoolConfig setMaxPoolSize (int maxPoolSize) {

    if (maxPoolSize < 0) {
      throw new IllegalArgumentException("Maximum pool size must be >= 0");
    }

    this.maxPoolSize.set(maxPoolSize);

    return this;
  }

  public long getAcquireWaitTimeMillis () {

    return acquireWaitTimeMillis.get();
  }

  public ConnectionPoolConfig setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    if (acquireWaitTimeMillis < 0) {
      throw new IllegalArgumentException("Acquire wait time must be >= 0");
    }

    this.acquireWaitTimeMillis.set(acquireWaitTimeMillis);

    return this;
  }

  public int getMaxLeaseTimeSeconds () {

    return maxLeaseTimeSeconds.get();
  }

  public ConnectionPoolConfig setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    if (maxLeaseTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum lease time must be >= 0");
    }

    this.maxLeaseTimeSeconds.set(maxLeaseTimeSeconds);

    return this;
  }

  public int getMaxIdleTimeSeconds () {

    return maxIdleTimeSeconds.get();
  }

  public ConnectionPoolConfig setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    if (maxIdleTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum idle time must be >= 0");
    }

    this.maxIdleTimeSeconds.set(maxIdleTimeSeconds);

    return this;
  }

  public int getUnreturnedConnectionTimeoutSeconds () {

    return unreturnedConnectionTimeoutSeconds.get();
  }

  public ConnectionPoolConfig setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

    if (unreturnedConnectionTimeoutSeconds < 0) {
      throw new IllegalArgumentException("Unreturned connection timeout must be >= 0");
    }

    this.unreturnedConnectionTimeoutSeconds.set(unreturnedConnectionTimeoutSeconds);

    return this;
  }
}

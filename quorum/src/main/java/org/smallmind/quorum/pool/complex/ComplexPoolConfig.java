/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.quorum.pool.complex;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.quorum.pool.PoolConfig;

public class ComplexPoolConfig extends PoolConfig<ComplexPoolConfig> {

  private final AtomicBoolean reportLeaseTimeNanos = new AtomicBoolean(false);
  private final AtomicBoolean testOnCreate = new AtomicBoolean(false);
  private final AtomicBoolean testOnAcquire = new AtomicBoolean(false);
  private final AtomicBoolean existentiallyAware = new AtomicBoolean(false);
  private final AtomicLong creationTimeoutMillis = new AtomicLong(0);
  private final AtomicInteger initialPoolSize = new AtomicInteger(0);
  private final AtomicInteger minPoolSize = new AtomicInteger(0);
  private final AtomicInteger maxLeaseTimeSeconds = new AtomicInteger(0);
  private final AtomicInteger maxIdleTimeSeconds = new AtomicInteger(0);
  private final AtomicInteger unReturnedElementTimeoutSeconds = new AtomicInteger(0);

  public ComplexPoolConfig () {

  }

  public ComplexPoolConfig (PoolConfig<?> poolConfig) {

    super(poolConfig);

    if (poolConfig.getConfigurationClass().isAssignableFrom(ComplexPoolConfig.class)) {
      setReportLeaseTimeNanos(((ComplexPoolConfig)poolConfig).isReportLeaseTimeNanos());
      setTestOnCreate(((ComplexPoolConfig)poolConfig).isTestOnCreate());
      setTestOnAcquire(((ComplexPoolConfig)poolConfig).isTestOnAcquire());
      setExistentiallyAware(((ComplexPoolConfig)poolConfig).isExistentiallyAware());
      setCreationTimeoutMillis(((ComplexPoolConfig)poolConfig).getCreationTimeoutMillis());
      setInitialPoolSize(((ComplexPoolConfig)poolConfig).getInitialPoolSize());
      setMinPoolSize(((ComplexPoolConfig)poolConfig).getMinPoolSize());
      setMaxLeaseTimeSeconds(((ComplexPoolConfig)poolConfig).getMaxLeaseTimeSeconds());
      setMaxIdleTimeSeconds(((ComplexPoolConfig)poolConfig).getMaxIdleTimeSeconds());
      setUnReturnedElementTimeoutSeconds(((ComplexPoolConfig)poolConfig).getUnReturnedElementTimeoutSeconds());
    }
  }

  @Override
  public Class<ComplexPoolConfig> getConfigurationClass () {

    return ComplexPoolConfig.class;
  }

  public boolean requiresDeconstruction () {

    return (getMaxLeaseTimeSeconds() > 0) || (getMaxIdleTimeSeconds() > 0) || (getUnReturnedElementTimeoutSeconds() > 0);
  }

  public boolean isReportLeaseTimeNanos () {

    return reportLeaseTimeNanos.get();
  }

  public ComplexPoolConfig setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    this.reportLeaseTimeNanos.set(reportLeaseTimeNanos);

    return getConfigurationClass().cast(this);
  }

  public boolean isExistentiallyAware () {

    return existentiallyAware.get();
  }

  public ComplexPoolConfig setExistentiallyAware (boolean existentiallyAware) {

    this.existentiallyAware.set(existentiallyAware);

    return getConfigurationClass().cast(this);
  }

  public boolean isTestOnCreate () {

    return testOnCreate.get();
  }

  public ComplexPoolConfig setTestOnCreate (boolean testOnCreate) {

    this.testOnCreate.set(testOnCreate);

    return getConfigurationClass().cast(this);
  }

  public boolean isTestOnAcquire () {

    return testOnAcquire.get();
  }

  public ComplexPoolConfig setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire.set(testOnAcquire);

    return getConfigurationClass().cast(this);
  }

  public int getInitialPoolSize () {

    return initialPoolSize.get();
  }

  public ComplexPoolConfig setInitialPoolSize (int initialPoolSize) {

    if (initialPoolSize < 0) {
      throw new IllegalArgumentException("Initial pool size must be >= 0");
    }

    this.initialPoolSize.set(initialPoolSize);

    return getConfigurationClass().cast(this);
  }

  public int getMinPoolSize () {

    return minPoolSize.get();
  }

  public ComplexPoolConfig setMinPoolSize (int minPoolSize) {

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size must be >= 0");
    }

    this.minPoolSize.set(minPoolSize);

    return getConfigurationClass().cast(this);
  }

  public long getCreationTimeoutMillis () {

    return creationTimeoutMillis.get();
  }

  public ComplexPoolConfig setCreationTimeoutMillis (long creationTimeoutMillis) {

    if (creationTimeoutMillis < 0) {
      throw new IllegalArgumentException("Creation timeout must be >= 0");
    }

    this.creationTimeoutMillis.set(creationTimeoutMillis);

    return getConfigurationClass().cast(this);
  }

  public int getMaxLeaseTimeSeconds () {

    return maxLeaseTimeSeconds.get();
  }

  public ComplexPoolConfig setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    if (maxLeaseTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum lease time must be >= 0");
    }

    this.maxLeaseTimeSeconds.set(maxLeaseTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  public int getMaxIdleTimeSeconds () {

    return maxIdleTimeSeconds.get();
  }

  public ComplexPoolConfig setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    if (maxIdleTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum idle time must be >= 0");
    }

    this.maxIdleTimeSeconds.set(maxIdleTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  public int getUnReturnedElementTimeoutSeconds () {

    return unReturnedElementTimeoutSeconds.get();
  }

  public ComplexPoolConfig setUnReturnedElementTimeoutSeconds (int unReturnedElementTimeoutSeconds) {

    if (unReturnedElementTimeoutSeconds < 0) {
      throw new IllegalArgumentException("Un-returned element timeout must be >= 0");
    }

    this.unReturnedElementTimeoutSeconds.set(unReturnedElementTimeoutSeconds);

    return getConfigurationClass().cast(this);
  }
}

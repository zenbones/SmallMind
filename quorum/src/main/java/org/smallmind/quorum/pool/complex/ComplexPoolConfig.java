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
package org.smallmind.quorum.pool.complex;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.quorum.pool.PoolConfig;

/**
 * Configuration for the complex component pool, including lifecycle validation, sizing,
 * and deconstruction timing.
 */
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
  private final AtomicInteger maxProcessingTimeSeconds = new AtomicInteger(0);

  /**
   * Creates a configuration with default values.
   */
  public ComplexPoolConfig () {

  }

  /**
   * Copy constructor that pulls values from another pool config.
   *
   * @param poolConfig configuration to copy
   */
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
      setMaxProcessingTimeSeconds(((ComplexPoolConfig)poolConfig).getMaxProcessingTimeSeconds());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<ComplexPoolConfig> getConfigurationClass () {

    return ComplexPoolConfig.class;
  }

  /**
   * Indicates whether any deconstruction fuse is required based on configured limits.
   *
   * @return {@code true} if lease, idle, or processing limits are set
   */
  public boolean requiresDeconstruction () {

    return (getMaxLeaseTimeSeconds() > 0) || (getMaxIdleTimeSeconds() > 0) || (getMaxProcessingTimeSeconds() > 0);
  }

  /**
   * Indicates whether lease time reporting is enabled.
   *
   * @return {@code true} if lease time metrics are emitted
   */
  public boolean isReportLeaseTimeNanos () {

    return reportLeaseTimeNanos.get();
  }

  /**
   * Enables or disables reporting of lease times in nanoseconds.
   *
   * @param reportLeaseTimeNanos whether to emit lease time metrics
   * @return this configuration instance
   */
  public ComplexPoolConfig setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    this.reportLeaseTimeNanos.set(reportLeaseTimeNanos);

    return getConfigurationClass().cast(this);
  }

  /**
   * Indicates whether existential stack trace capture is enabled.
   *
   * @return {@code true} if existential tracking is on
   */
  public boolean isExistentiallyAware () {

    return existentiallyAware.get();
  }

  /**
   * Enables or disables existential tracking of component stack traces.
   *
   * @param existentiallyAware {@code true} to capture stack traces
   * @return this configuration instance
   */
  public ComplexPoolConfig setExistentiallyAware (boolean existentiallyAware) {

    this.existentiallyAware.set(existentiallyAware);

    return getConfigurationClass().cast(this);
  }

  /**
   * Indicates whether components are validated immediately after creation.
   *
   * @return {@code true} if validation is enabled on create
   */
  public boolean isTestOnCreate () {

    return testOnCreate.get();
  }

  /**
   * Sets whether components should be validated immediately after creation.
   *
   * @param testOnCreate {@code true} to validate on creation
   * @return this configuration instance
   */
  public ComplexPoolConfig setTestOnCreate (boolean testOnCreate) {

    this.testOnCreate.set(testOnCreate);

    return getConfigurationClass().cast(this);
  }

  /**
   * Indicates whether components are validated when acquired from the pool.
   *
   * @return {@code true} if validation is enabled on acquire
   */
  public boolean isTestOnAcquire () {

    return testOnAcquire.get();
  }

  /**
   * Sets whether components should be validated on acquisition from the pool.
   *
   * @param testOnAcquire {@code true} to validate before handing out
   * @return this configuration instance
   */
  public ComplexPoolConfig setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire.set(testOnAcquire);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the configured initial pool size.
   *
   * @return number of instances to pre-create
   */
  public int getInitialPoolSize () {

    return initialPoolSize.get();
  }

  /**
   * Sets the initial number of components to create during startup.
   *
   * @param initialPoolSize number of instances to pre-create; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setInitialPoolSize (int initialPoolSize) {

    if (initialPoolSize < 0) {
      throw new IllegalArgumentException("Initial pool size must be >= 0");
    }

    this.initialPoolSize.set(initialPoolSize);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the minimum pool size.
   *
   * @return minimum number of components to retain
   */
  public int getMinPoolSize () {

    return minPoolSize.get();
  }

  /**
   * Sets the minimum number of components to maintain in the pool.
   *
   * @param minPoolSize minimum pool size; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setMinPoolSize (int minPoolSize) {

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size must be >= 0");
    }

    this.minPoolSize.set(minPoolSize);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the creation timeout in milliseconds.
   *
   * @return creation timeout
   */
  public long getCreationTimeoutMillis () {

    return creationTimeoutMillis.get();
  }

  /**
   * Sets the timeout in milliseconds for creating a new component instance.
   *
   * @param creationTimeoutMillis timeout in milliseconds; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setCreationTimeoutMillis (long creationTimeoutMillis) {

    if (creationTimeoutMillis < 0) {
      throw new IllegalArgumentException("Creation timeout must be >= 0");
    }

    this.creationTimeoutMillis.set(creationTimeoutMillis);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the maximum lease time in seconds.
   *
   * @return lease timeout in seconds
   */
  public int getMaxLeaseTimeSeconds () {

    return maxLeaseTimeSeconds.get();
  }

  /**
   * Sets the maximum lease time (seconds) before a component must be reclaimed.
   *
   * @param maxLeaseTimeSeconds max lease in seconds; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    if (maxLeaseTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum lease time must be >= 0");
    }

    this.maxLeaseTimeSeconds.set(maxLeaseTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the maximum idle time in seconds.
   *
   * @return idle timeout in seconds
   */
  public int getMaxIdleTimeSeconds () {

    return maxIdleTimeSeconds.get();
  }

  /**
   * Sets the maximum idle time (seconds) before a component is deconstructed.
   *
   * @param maxIdleTimeSeconds max idle in seconds; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    if (maxIdleTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum idle time must be >= 0");
    }

    this.maxIdleTimeSeconds.set(maxIdleTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  /**
   * Retrieves the maximum processing time in seconds.
   *
   * @return processing timeout in seconds
   */
  public int getMaxProcessingTimeSeconds () {

    return maxProcessingTimeSeconds.get();
  }

  /**
   * Sets the maximum processing time (seconds) an element can be checked out before being timed out.
   *
   * @param maxProcessingTimeSeconds max processing time in seconds; must be non-negative
   * @return this configuration instance
   * @throws IllegalArgumentException if the value is negative
   */
  public ComplexPoolConfig setMaxProcessingTimeSeconds (int maxProcessingTimeSeconds) {

    if (maxProcessingTimeSeconds < 0) {
      throw new IllegalArgumentException("Un-returned element timeout must be >= 0");
    }

    this.maxProcessingTimeSeconds.set(maxProcessingTimeSeconds);

    return getConfigurationClass().cast(this);
  }
}

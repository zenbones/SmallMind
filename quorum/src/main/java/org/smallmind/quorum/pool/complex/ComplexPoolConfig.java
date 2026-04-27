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
 * Extended configuration for the complex component pool, adding lifecycle validation, size
 * pre-warming, deconstruction timeouts, and metrics control on top of the base
 * {@link PoolConfig} properties.
 * <p>
 * All fields are stored in atomic references so that live changes are visible across threads
 * without additional synchronization. Every setter validates its argument and throws
 * {@link IllegalArgumentException} for negative values. Setters return {@code this} for
 * fluent chaining.
 * <p>
 * <strong>Deconstruction:</strong> when any of {@code maxLeaseTimeSeconds},
 * {@code maxIdleTimeSeconds}, or {@code maxProcessingTimeSeconds} is greater than zero, the pool
 * attaches {@link DeconstructionFuse} instances to each pin. {@link #requiresDeconstruction()}
 * reflects this condition.
 * <p>
 * <strong>Defaults:</strong> all flags {@code false}; all numeric limits {@code 0}
 * (disabled / unbounded).
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
   * Creates a configuration with all defaults.
   */
  public ComplexPoolConfig () {

  }

  /**
   * Copy constructor that applies all base-class and, when applicable, all complex-specific
   * properties from {@code poolConfig}.
   * <p>
   * Complex-specific values are copied only when {@code poolConfig} is itself a
   * {@link ComplexPoolConfig} (checked via assignability to avoid class-loader issues).
   *
   * @param poolConfig the source configuration; must not be {@code null}
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
   * Returns the runtime class of the concrete configuration subclass.
   * <p>
   * Used by the fluent setters to cast {@code this} to {@code P} before returning.
   *
   * @return the concrete configuration class
   */
  @Override
  public Class<ComplexPoolConfig> getConfigurationClass () {

    return ComplexPoolConfig.class;
  }

  /**
   * Returns {@code true} when at least one deconstruction timeout — lease, idle, or processing —
   * is configured with a positive value, indicating that the pool should attach
   * {@link DeconstructionFuse} instances to its pins.
   *
   * @return {@code true} if any deconstruction limit is active
   */
  public boolean requiresDeconstruction () {

    return (getMaxLeaseTimeSeconds() > 0) || (getMaxIdleTimeSeconds() > 0) || (getMaxProcessingTimeSeconds() > 0);
  }

  /**
   * Returns whether the pool fires lease-time events and Claxon metrics when a component
   * is returned.
   *
   * @return {@code true} if per-component lease-time reporting is enabled
   */
  public boolean isReportLeaseTimeNanos () {

    return reportLeaseTimeNanos.get();
  }

  /**
   * Enables or disables per-component lease-time reporting via events and Claxon metrics.
   *
   * @param reportLeaseTimeNanos {@code true} to enable reporting
   * @return this configuration instance for fluent chaining
   */
  public ComplexPoolConfig setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    this.reportLeaseTimeNanos.set(reportLeaseTimeNanos);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns whether the pool captures the stack trace of the thread that acquired each
   * component, enabling later diagnosis of leaked or long-held components.
   *
   * @return {@code true} if existential stack-trace capture is enabled
   */
  public boolean isExistentiallyAware () {

    return existentiallyAware.get();
  }

  /**
   * Enables or disables stack-trace capture at component-acquisition time.
   *
   * @param existentiallyAware {@code true} to capture stack traces on acquire
   * @return this configuration instance for fluent chaining
   */
  public ComplexPoolConfig setExistentiallyAware (boolean existentiallyAware) {

    this.existentiallyAware.set(existentiallyAware);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns whether newly created components are validated before being placed into service.
   *
   * @return {@code true} if validation on creation is enabled
   */
  public boolean isTestOnCreate () {

    return testOnCreate.get();
  }

  /**
   * Enables or disables validation of newly created components.
   * <p>
   * When enabled, a component that fails validation after creation causes a
   * {@link ComponentValidationException} and the component is discarded.
   *
   * @param testOnCreate {@code true} to validate on creation
   * @return this configuration instance for fluent chaining
   */
  public ComplexPoolConfig setTestOnCreate (boolean testOnCreate) {

    this.testOnCreate.set(testOnCreate);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns whether components are validated when they are taken from the free queue for
   * a caller.
   *
   * @return {@code true} if validation on acquire is enabled
   */
  public boolean isTestOnAcquire () {

    return testOnAcquire.get();
  }

  /**
   * Enables or disables validation of components at acquisition time.
   * <p>
   * When enabled, a component that fails validation is removed and a different component is
   * sought; creation of a new instance may be triggered if the free queue is exhausted.
   *
   * @param testOnAcquire {@code true} to validate before handing a component to a caller
   * @return this configuration instance for fluent chaining
   */
  public ComplexPoolConfig setTestOnAcquire (boolean testOnAcquire) {

    this.testOnAcquire.set(testOnAcquire);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the number of component instances to create eagerly during
   * {@link ComponentPool#startup()}.
   *
   * @return the initial pool size; {@code 0} means no eager creation
   */
  public int getInitialPoolSize () {

    return initialPoolSize.get();
  }

  /**
   * Sets the number of component instances to create eagerly during pool startup.
   *
   * @param initialPoolSize number of instances to pre-create; must be non-negative
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code initialPoolSize} is negative
   */
  public ComplexPoolConfig setInitialPoolSize (int initialPoolSize) {

    if (initialPoolSize < 0) {
      throw new IllegalArgumentException("Initial pool size must be >= 0");
    }

    this.initialPoolSize.set(initialPoolSize);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the minimum number of instances the pool attempts to maintain at all times.
   * <p>
   * When a component is terminated the pool replaces it if doing so would keep the count at
   * or above this floor.
   *
   * @return the minimum pool size; {@code 0} disables the floor
   */
  public int getMinPoolSize () {

    return minPoolSize.get();
  }

  /**
   * Sets the minimum pool size floor.
   *
   * @param minPoolSize minimum number of instances to retain; must be non-negative
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code minPoolSize} is negative
   */
  public ComplexPoolConfig setMinPoolSize (int minPoolSize) {

    if (minPoolSize < 0) {
      throw new IllegalArgumentException("Minimum pool size must be >= 0");
    }

    this.minPoolSize.set(minPoolSize);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the maximum time in milliseconds the pool will wait for a new component instance
   * to be constructed before abandoning the attempt.
   * <p>
   * A value of {@code 0} means there is no timeout; the factory call blocks indefinitely.
   *
   * @return the creation timeout in milliseconds; {@code 0} for no timeout
   */
  public long getCreationTimeoutMillis () {

    return creationTimeoutMillis.get();
  }

  /**
   * Sets the creation timeout.
   *
   * @param creationTimeoutMillis timeout in milliseconds; must be non-negative; {@code 0}
   *                              disables the timeout
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code creationTimeoutMillis} is negative
   */
  public ComplexPoolConfig setCreationTimeoutMillis (long creationTimeoutMillis) {

    if (creationTimeoutMillis < 0) {
      throw new IllegalArgumentException("Creation timeout must be >= 0");
    }

    this.creationTimeoutMillis.set(creationTimeoutMillis);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the maximum wall-clock time in seconds that a component may remain leased to a
   * caller before the pool's {@link MaxLeaseTimeDeconstructionFuse} ignites and triggers
   * reclamation.
   *
   * @return the max lease time in seconds; {@code 0} means no lease limit
   */
  public int getMaxLeaseTimeSeconds () {

    return maxLeaseTimeSeconds.get();
  }

  /**
   * Sets the maximum lease time limit.
   *
   * @param maxLeaseTimeSeconds max lease duration in seconds; must be non-negative; {@code 0}
   *                            disables the limit
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code maxLeaseTimeSeconds} is negative
   */
  public ComplexPoolConfig setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    if (maxLeaseTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum lease time must be >= 0");
    }

    this.maxLeaseTimeSeconds.set(maxLeaseTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the maximum time in seconds a component may sit on the free queue without being
   * requested before the pool's {@link MaxIdleTimeDeconstructionFuse} ignites and retires it.
   *
   * @return the max idle time in seconds; {@code 0} means no idle limit
   */
  public int getMaxIdleTimeSeconds () {

    return maxIdleTimeSeconds.get();
  }

  /**
   * Sets the maximum idle time limit.
   *
   * @param maxIdleTimeSeconds max idle duration in seconds; must be non-negative; {@code 0}
   *                           disables the limit
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code maxIdleTimeSeconds} is negative
   */
  public ComplexPoolConfig setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    if (maxIdleTimeSeconds < 0) {
      throw new IllegalArgumentException("Maximum idle time must be >= 0");
    }

    this.maxIdleTimeSeconds.set(maxIdleTimeSeconds);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the maximum time in seconds a component may be actively processing (checked out)
   * before the pool's {@link MaxProcessingTimeDeconstructionFuse} ignites and forcibly
   * terminates it.
   * <p>
   * Unlike the lease and idle limits this limit is <em>prejudicial</em>: the fuse fires even
   * if the caller has not yet returned the component.
   *
   * @return the max processing time in seconds; {@code 0} means no processing limit
   */
  public int getMaxProcessingTimeSeconds () {

    return maxProcessingTimeSeconds.get();
  }

  /**
   * Sets the maximum processing time limit.
   *
   * @param maxProcessingTimeSeconds max processing duration in seconds; must be non-negative;
   *                                 {@code 0} disables the limit
   * @return this configuration instance for fluent chaining
   * @throws IllegalArgumentException if {@code maxProcessingTimeSeconds} is negative
   */
  public ComplexPoolConfig setMaxProcessingTimeSeconds (int maxProcessingTimeSeconds) {

    if (maxProcessingTimeSeconds < 0) {
      throw new IllegalArgumentException("Un-returned element timeout must be >= 0");
    }

    this.maxProcessingTimeSeconds.set(maxProcessingTimeSeconds);

    return getConfigurationClass().cast(this);
  }
}

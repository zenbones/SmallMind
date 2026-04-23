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
package org.smallmind.quorum.pool;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Shared base configuration for pool implementations, holding limits on pool size and how long
 * callers will wait when no component is immediately available.
 * <p>
 * Both properties are stored in atomic fields so that live changes to a running pool are visible
 * without additional synchronization on the caller's side. Setters return the concrete
 * configuration type {@code P} to support fluent-style chaining.
 * <p>
 * Defaults: {@code maxPoolSize = 10}, {@code acquireWaitTimeMillis = 0} (do not wait).
 *
 * @param <P> the concrete configuration subclass, used to type the fluent setters
 */
public abstract class PoolConfig<P extends PoolConfig> {

  private final AtomicLong acquireWaitTimeMillis = new AtomicLong(0);
  private final AtomicInteger maxPoolSize = new AtomicInteger(10);

  /**
   * Creates a configuration with default values ({@code maxPoolSize=10}, {@code acquireWaitTimeMillis=0}).
   */
  public PoolConfig () {

  }

  /**
   * Copy constructor that copies the pool size and acquire wait time from {@code poolConfig}.
   *
   * @param poolConfig the source configuration to copy from
   */
  public PoolConfig (PoolConfig<?> poolConfig) {

    setAcquireWaitTimeMillis(poolConfig.getAcquireWaitTimeMillis());
    setMaxPoolSize(poolConfig.getMaxPoolSize());
  }

  /**
   * Returns the runtime class of the concrete configuration subclass.
   * <p>
   * Used by the fluent setters to cast {@code this} to {@code P} before returning.
   *
   * @return the concrete configuration class
   */
  public abstract Class<P> getConfigurationClass ();

  /**
   * Returns the maximum number of component instances the pool will hold concurrently.
   * <p>
   * A value of {@code 0} means unbounded.
   *
   * @return the pool size cap; {@code 0} for unbounded
   */
  public int getMaxPoolSize () {

    return maxPoolSize.get();
  }

  /**
   * Sets the maximum number of component instances the pool will hold concurrently.
   * <p>
   * A value of {@code 0} means unbounded.
   *
   * @param maxPoolSize the new pool size cap; must be non-negative
   * @return this configuration instance cast to {@code P}, for fluent chaining
   * @throws IllegalArgumentException if {@code maxPoolSize} is negative
   */
  public P setMaxPoolSize (int maxPoolSize) {

    if (maxPoolSize < 0) {
      throw new IllegalArgumentException("Maximum pool size must be >= 0");
    }

    this.maxPoolSize.set(maxPoolSize);

    return getConfigurationClass().cast(this);
  }

  /**
   * Returns the maximum time in milliseconds a caller will block waiting for a component when the
   * pool is at capacity.
   * <p>
   * A value of {@code 0} means the pool will not wait; it will throw immediately if no component
   * is free.
   *
   * @return the wait time in milliseconds; {@code 0} for no waiting
   */
  public long getAcquireWaitTimeMillis () {

    return acquireWaitTimeMillis.get();
  }

  /**
   * Sets the maximum time in milliseconds a caller will block waiting for a component when the
   * pool is at capacity.
   * <p>
   * A value of {@code 0} means the pool will not wait.
   *
   * @param acquireWaitTimeMillis the new wait time in milliseconds; must be non-negative
   * @return this configuration instance cast to {@code P}, for fluent chaining
   * @throws IllegalArgumentException if {@code acquireWaitTimeMillis} is negative
   */
  public P setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    if (acquireWaitTimeMillis < 0) {
      throw new IllegalArgumentException("Acquire wait time must be >= 0");
    }

    this.acquireWaitTimeMillis.set(acquireWaitTimeMillis);

    return getConfigurationClass().cast(this);
  }
}

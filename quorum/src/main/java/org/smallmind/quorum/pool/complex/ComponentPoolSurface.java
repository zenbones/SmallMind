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

import org.smallmind.quorum.pool.ComponentPoolException;

/**
 * Management and introspection interface for a complex component pool.
 * <p>
 * Exposes the full set of operations needed to start, stop, query, and reconfigure a pool at
 * runtime without requiring a dependency on the concrete {@link ComponentPool} class.
 * Implemented by {@link org.smallmind.quorum.pool.complex.jmx.ComponentPoolMonitor} so that
 * pool state can be observed and tuned through JMX without restarting the pool.
 */
public interface ComponentPoolSurface {

  /**
   * Returns the unique name that identifies this pool in metrics, JMX, and log output.
   *
   * @return the pool name
   */
  String getPoolName ();

  /**
   * Starts the pool, initialising the factory and pre-populating components.
   *
   * @throws ComponentPoolException if any part of the startup sequence fails
   */
  void startup ()
    throws ComponentPoolException;

  /**
   * Shuts down the pool, terminating all managed components and releasing factory resources.
   *
   * @throws ComponentPoolException if any part of the shutdown sequence fails
   */
  void shutdown ()
    throws ComponentPoolException;

  /**
   * Returns the total number of component instances managed by the pool (free + processing).
   *
   * @return the pool size
   */
  int getPoolSize ();

  /**
   * Returns the number of component instances currently idle on the free queue.
   *
   * @return the free-queue size
   */
  int getFreeSize ();

  /**
   * Returns the number of component instances currently checked out by callers.
   *
   * @return the processing count
   */
  int getProcessingSize ();

  /**
   * Returns whether newly created component instances are validated before entering service.
   *
   * @return {@code true} if validate-on-create is enabled
   */
  boolean isTestOnCreate ();

  /**
   * Enables or disables validation of component instances immediately after creation.
   *
   * @param testOnCreate {@code true} to validate each instance at creation time
   */
  void setTestOnCreate (boolean testOnCreate);

  /**
   * Returns whether component instances are validated when taken from the free queue for
   * a caller.
   *
   * @return {@code true} if validate-on-acquire is enabled
   */
  boolean isTestOnAcquire ();

  /**
   * Enables or disables validation of component instances at acquisition time.
   *
   * @param testOnAcquire {@code true} to validate before handing an instance to a caller
   */
  void setTestOnAcquire (boolean testOnAcquire);

  /**
   * Returns whether the pool fires per-component lease-time events and emits Claxon metrics
   * when a component is returned.
   *
   * @return {@code true} if lease-time reporting is enabled
   */
  boolean isReportLeaseTimeNanos ();

  /**
   * Enables or disables lease-time reporting.
   *
   * @param reportLeaseTimeNanos {@code true} to enable per-return lease-time events
   */
  void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos);

  /**
   * Returns whether the pool captures stack traces of the threads that acquire components,
   * enabling diagnosis of leaked or long-held components.
   *
   * @return {@code true} if existential stack-trace capture is enabled
   */
  boolean isExistentiallyAware ();

  /**
   * Enables or disables existential stack-trace capture.
   *
   * @param existentiallyAware {@code true} to record the acquiring thread's stack trace
   */
  void setExistentiallyAware (boolean existentiallyAware);

  /**
   * Returns the maximum time in milliseconds the pool allows for constructing a single
   * component instance. {@code 0} means no limit.
   *
   * @return the creation timeout in milliseconds
   */
  long getCreationTimeoutMillis ();

  /**
   * Sets the creation timeout in milliseconds.
   *
   * @param creationTimeoutMillis timeout; {@code 0} for no limit
   */
  void setCreationTimeoutMillis (long creationTimeoutMillis);

  /**
   * Returns the maximum time in milliseconds a caller may block waiting for a component
   * when the pool is at capacity. {@code 0} means throw immediately.
   *
   * @return the acquire wait time in milliseconds
   */
  long getAcquireWaitTimeMillis ();

  /**
   * Sets the acquire wait time in milliseconds.
   *
   * @param acquireWaitTimeMillis wait time; {@code 0} for immediate failure
   */
  void setAcquireWaitTimeMillis (long acquireWaitTimeMillis);

  /**
   * Returns the number of component instances pre-created at pool startup.
   *
   * @return the initial pool size
   */
  int getInitialPoolSize ();

  /**
   * Returns the minimum number of component instances the pool attempts to maintain.
   *
   * @return the minimum pool size; {@code 0} disables the floor
   */
  int getMinPoolSize ();

  /**
   * Sets the minimum pool size floor.
   *
   * @param minPoolSize minimum instances to keep alive; must be non-negative
   */
  void setMinPoolSize (int minPoolSize);

  /**
   * Returns the maximum number of component instances the pool will hold concurrently.
   * {@code 0} means unbounded.
   *
   * @return the maximum pool size
   */
  int getMaxPoolSize ();

  /**
   * Sets the maximum pool size cap.
   *
   * @param maxPoolSize maximum instances; {@code 0} for unbounded
   */
  void setMaxPoolSize (int maxPoolSize);

  /**
   * Returns the maximum wall-clock time in seconds a component may be held by a caller
   * before a lease fuse ignites and triggers non-prejudicial reclamation.
   *
   * @return the maximum lease time in seconds; {@code 0} means no limit
   */
  int getMaxLeaseTimeSeconds ();

  /**
   * Sets the maximum lease time in seconds.
   *
   * @param maxLeaseTimeSeconds lease limit; {@code 0} to disable
   */
  void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds);

  /**
   * Returns the maximum time in seconds a component may sit idle before an idle fuse ignites
   * and the component is retired.
   *
   * @return the maximum idle time in seconds; {@code 0} means no limit
   */
  int getMaxIdleTimeSeconds ();

  /**
   * Sets the maximum idle time in seconds.
   *
   * @param maxIdleTimeSeconds idle limit; {@code 0} to disable
   */
  void setMaxIdleTimeSeconds (int maxIdleTimeSeconds);

  /**
   * Returns the maximum time in seconds a component may be actively processing (checked out)
   * before a prejudicial processing-timeout fuse forcibly terminates it.
   *
   * @return the maximum processing time in seconds; {@code 0} means no limit
   */
  int getMaxProcessingTimeSeconds ();

  /**
   * Sets the maximum processing time in seconds.
   *
   * @param maxProcessingTimeSeconds processing limit; {@code 0} to disable
   */
  void setMaxProcessingTimeSeconds (int maxProcessingTimeSeconds);
}

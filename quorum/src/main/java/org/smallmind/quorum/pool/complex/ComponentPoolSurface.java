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
 * Management surface for controlling and querying a complex component pool.
 */
public interface ComponentPoolSurface {

  /**
   * Pool name for identification.
   *
   * @return pool name
   */
  String getPoolName ();

  /**
   * Starts the pool.
   *
   * @throws ComponentPoolException if startup fails
   */
  void startup ()
    throws ComponentPoolException;

  /**
   * Shuts down the pool.
   *
   * @throws ComponentPoolException if shutdown fails
   */
  void shutdown ()
    throws ComponentPoolException;

  /**
   * Total size of the pool.
   *
   * @return number of components
   */
  int getPoolSize ();

  /**
   * Number of free components.
   *
   * @return free count
   */
  int getFreeSize ();

  /**
   * Number of components currently processing.
   *
   * @return processing count
   */
  int getProcessingSize ();

  /**
   * Whether validation occurs upon creation.
   *
   * @return {@code true} if enabled
   */
  boolean isTestOnCreate ();

  /**
   * Enables or disables validation on creation.
   *
   * @param testOnCreate {@code true} to validate on create
   */
  void setTestOnCreate (boolean testOnCreate);

  /**
   * Whether validation occurs upon acquire.
   *
   * @return {@code true} if enabled
   */
  boolean isTestOnAcquire ();

  /**
   * Enables or disables validation on acquire.
   *
   * @param testOnAcquire {@code true} to validate on acquire
   */
  void setTestOnAcquire (boolean testOnAcquire);

  /**
   * Whether lease time should be reported in nanoseconds.
   *
   * @return {@code true} if reporting is enabled
   */
  boolean isReportLeaseTimeNanos ();

  /**
   * Enables or disables lease time reporting.
   *
   * @param reportLeaseTimeNanos {@code true} to enable reporting
   */
  void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos);

  /**
   * Whether existential tracking is enabled.
   *
   * @return {@code true} if existential tracking is on
   */
  boolean isExistentiallyAware ();

  /**
   * Enables or disables existential tracking.
   *
   * @param existentiallyAware {@code true} to enable tracking
   */
  void setExistentiallyAware (boolean existentiallyAware);

  /**
   * Timeout for creating new components (ms).
   *
   * @return creation timeout in milliseconds
   */
  long getCreationTimeoutMillis ();

  /**
   * Sets the creation timeout in milliseconds.
   *
   * @param creationTimeoutMillis timeout in milliseconds
   */
  void setCreationTimeoutMillis (long creationTimeoutMillis);

  /**
   * Wait time for acquiring a component (ms).
   *
   * @return acquire wait in milliseconds
   */
  long getAcquireWaitTimeMillis ();

  /**
   * Sets the acquire wait time in milliseconds.
   *
   * @param acquireWaitTimeMillis wait in milliseconds
   */
  void setAcquireWaitTimeMillis (long acquireWaitTimeMillis);

  /**
   * Initial pool size.
   *
   * @return number of elements created on startup
   */
  int getInitialPoolSize ();

  /**
   * Minimum pool size.
   *
   * @return minimum number of elements to maintain
   */
  int getMinPoolSize ();

  /**
   * Sets the minimum pool size.
   *
   * @param minPoolSize minimum size
   */
  void setMinPoolSize (int minPoolSize);

  /**
   * Maximum pool size (0 for unbounded).
   *
   * @return maximum size
   */
  int getMaxPoolSize ();

  /**
   * Sets the maximum pool size.
   *
   * @param maxPoolSize maximum size (0 for unbounded)
   */
  void setMaxPoolSize (int maxPoolSize);

  /**
   * Maximum lease time in seconds.
   *
   * @return lease timeout
   */
  int getMaxLeaseTimeSeconds ();

  /**
   * Sets the maximum lease time in seconds.
   *
   * @param maxLeaseTimeSeconds lease timeout
   */
  void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds);

  /**
   * Maximum idle time in seconds.
   *
   * @return idle timeout
   */
  int getMaxIdleTimeSeconds ();

  /**
   * Sets the maximum idle time in seconds.
   *
   * @param maxIdleTimeSeconds idle timeout
   */
  void setMaxIdleTimeSeconds (int maxIdleTimeSeconds);

  /**
   * Maximum processing time in seconds.
   *
   * @return processing timeout
   */
  int getMaxProcessingTimeSeconds ();

  /**
   * Sets the maximum processing time in seconds.
   *
   * @param maxProcessingTimeSeconds processing timeout
   */
  void setMaxProcessingTimeTimeSeconds (int maxProcessingTimeSeconds);
}

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
package org.smallmind.quorum.pool.complex.jmx;

import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;

/**
 * JMX monitor exposing configuration and metrics for a {@link ComponentPool}. Emits notifications
 * for creation errors and lease time reporting.
 */
public class ComponentPoolMonitor extends NotificationBroadcasterSupport implements ComponentPoolMonitorMXBean, MBeanRegistration, ComponentPoolEventListener {

  private final ComponentPool<?> componentPool;
  private ObjectName objectName;

  /**
   * Creates the monitor for the given pool.
   *
   * @param componentPool pool to expose via JMX
   */
  public ComponentPoolMonitor (ComponentPool<?> componentPool) {

    super(new MBeanNotificationInfo(new String[] {CreationErrorOccurredNotification.TYPE}, CreationErrorOccurredNotification.class.getName(), "Creation Error Occurred"), new MBeanNotificationInfo(new String[] {ComponentLeaseTimeNotification.TYPE}, ComponentLeaseTimeNotification.class.getName(), "Component Lease Time"));

    this.componentPool = componentPool;
  }

  /**
   * Registers this monitor and attaches as a pool listener.
   *
   * @param mBeanServer server registering the bean
   * @param objectName  proposed object name
   * @return the object name used for registration
   */
  public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName) {

    componentPool.addComponentPoolEventListener(this);

    return this.objectName = objectName;
  }

  /**
   * Post-registration hook (no-op).
   *
   * @param success whether registration succeeded
   */
  public void postRegister (Boolean success) {

  }

  /**
   * Pre-deregistration hook that removes pool listener registration.
   */
  public void preDeregister () {

    componentPool.removeComponentPoolEventListener(this);
  }

  /**
   * Post-deregistration hook (no-op).
   */
  public void postDeregister () {

  }

  /**
   * Handles pool error events by sending a JMX notification.
   */
  @Override
  public void reportErrorOccurred (ErrorReportingComponentPoolEvent<?> event) {

    sendNotification(new CreationErrorOccurredNotification(objectName, event.getException()));
  }

  /**
   * Handles lease time events by sending a JMX notification.
   */
  @Override
  public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent<?> event) {

    sendNotification(new ComponentLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
  }

  /**
   * Returns the name of the monitored pool.
   *
   * @return pool name
   */
  public String getPoolName () {

    return componentPool.getPoolName();
  }

  /**
   * Starts the underlying pool.
   *
   * @throws ComponentPoolException if startup fails
   */
  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  /**
   * Shuts down the underlying pool.
   *
   * @throws ComponentPoolException if shutdown fails
   */
  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }

  /**
   * Whether components are validated on creation.
   *
   * @return {@code true} if validation occurs on create
   */
  public boolean isTestOnCreate () {

    return componentPool.getComplexPoolConfig().isTestOnCreate();
  }

  /**
   * Sets validation on creation.
   *
   * @param testOnCreate {@code true} to validate on create
   */
  public void setTestOnCreate (boolean testOnCreate) {

    componentPool.getComplexPoolConfig().setTestOnCreate(testOnCreate);
  }

  /**
   * Whether components are validated on acquire.
   *
   * @return {@code true} if validation occurs on acquire
   */
  public boolean isTestOnAcquire () {

    return componentPool.getComplexPoolConfig().isTestOnAcquire();
  }

  /**
   * Sets validation on acquire.
   *
   * @param testOnAcquire {@code true} to validate on acquire
   */
  public void setTestOnAcquire (boolean testOnAcquire) {

    componentPool.getComplexPoolConfig().setTestOnAcquire(testOnAcquire);
  }

  /**
   * Whether lease time reporting is enabled.
   *
   * @return {@code true} if reporting is enabled
   */
  public boolean isReportLeaseTimeNanos () {

    return componentPool.getComplexPoolConfig().isReportLeaseTimeNanos();
  }

  /**
   * Enables or disables lease time reporting.
   *
   * @param reportLeaseTimeNanos {@code true} to enable reporting
   */
  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    componentPool.getComplexPoolConfig().setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  /**
   * Whether existential awareness is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isExistentiallyAware () {

    return componentPool.getComplexPoolConfig().isExistentiallyAware();
  }

  /**
   * Enables or disables existential awareness.
   *
   * @param existentiallyAware {@code true} to enable tracking
   */
  public void setExistentiallyAware (boolean existentiallyAware) {

    componentPool.getComplexPoolConfig().setExistentiallyAware(existentiallyAware);
  }

  /**
   * Returns the creation timeout in milliseconds.
   *
   * @return creation timeout
   */
  public long getCreationTimeoutMillis () {

    return componentPool.getComplexPoolConfig().getCreationTimeoutMillis();
  }

  /**
   * Sets the creation timeout in milliseconds.
   *
   * @param creationTimeoutMillis timeout in milliseconds
   */
  public void setCreationTimeoutMillis (long creationTimeoutMillis) {

    componentPool.getComplexPoolConfig().setCreationTimeoutMillis(creationTimeoutMillis);
  }

  /**
   * Gets the initial pool size.
   *
   * @return initial size
   */
  public int getInitialPoolSize () {

    return componentPool.getComplexPoolConfig().getInitialPoolSize();
  }

  /**
   * Gets the minimum pool size.
   *
   * @return minimum size
   */
  public int getMinPoolSize () {

    return componentPool.getComplexPoolConfig().getMinPoolSize();
  }

  /**
   * Sets the minimum pool size.
   *
   * @param minPoolSize minimum size
   */
  public void setMinPoolSize (int minPoolSize) {

    componentPool.getComplexPoolConfig().setMinPoolSize(minPoolSize);
  }

  /**
   * Gets the maximum pool size (0 for unbounded).
   *
   * @return maximum size
   */
  public int getMaxPoolSize () {

    return componentPool.getComplexPoolConfig().getMaxPoolSize();
  }

  /**
   * Sets the maximum pool size.
   *
   * @param maxPoolSize maximum size (0 for unbounded)
   */
  public void setMaxPoolSize (int maxPoolSize) {

    componentPool.getComplexPoolConfig().setMaxPoolSize(maxPoolSize);
  }

  /**
   * Gets the acquire wait time in milliseconds.
   *
   * @return wait time
   */
  public synchronized long getAcquireWaitTimeMillis () {

    return componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
  }

  /**
   * Sets the acquire wait time in milliseconds.
   *
   * @param acquireWaitTimeMillis wait time
   */
  public synchronized void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    componentPool.getComplexPoolConfig().setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  /**
   * Gets the maximum lease time in seconds.
   *
   * @return maximum lease time
   */
  public int getMaxLeaseTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds();
  }

  /**
   * Sets the maximum lease time in seconds.
   *
   * @param leaseTimeSeconds lease timeout
   */
  public void setMaxLeaseTimeSeconds (int leaseTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxLeaseTimeSeconds(leaseTimeSeconds);
  }

  /**
   * Gets the maximum idle time in seconds.
   *
   * @return maximum idle time
   */
  public int getMaxIdleTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds();
  }

  /**
   * Sets the maximum idle time in seconds.
   *
   * @param maxIdleTimeSeconds idle timeout
   */
  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  /**
   * Gets the maximum processing time in seconds.
   *
   * @return maximum processing time
   */
  public int getMaxProcessingTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds();
  }

  /**
   * Sets the maximum processing time in seconds.
   *
   * @param maxProcessingTimeSeconds processing timeout
   */
  public void setMaxProcessingTimeTimeSeconds (int maxProcessingTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxProcessingTimeSeconds(maxProcessingTimeSeconds);
  }

  /**
   * Returns the current pool size.
   *
   * @return pool size
   */
  public int getPoolSize () {

    return componentPool.getPoolSize();
  }

  /**
   * Returns the number of free components.
   *
   * @return free size
   */
  public int getFreeSize () {

    return componentPool.getFreeSize();
  }

  /**
   * Returns the number of components currently processing.
   *
   * @return processing size
   */
  public int getProcessingSize () {

    return componentPool.getProcessingSize();
  }
}

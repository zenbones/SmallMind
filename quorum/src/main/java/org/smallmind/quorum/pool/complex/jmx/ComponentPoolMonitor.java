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
 * JMX MBean that bridges a {@link ComponentPool} to the platform MBean server.
 * <p>
 * Implements {@link ComponentPoolMonitorMXBean} to expose the full pool management surface
 * (sizes, timeouts, feature flags, lifecycle operations), {@link MBeanRegistration} to
 * register itself as a pool event listener when the MBean is registered and deregister when
 * it is removed, and {@link ComponentPoolEventListener} to convert pool events into typed
 * JMX notifications:
 * <ul>
 *   <li>Pool errors generate a {@link CreationErrorOccurredNotification}.</li>
 *   <li>Lease-time events generate a {@link ComponentLeaseTimeNotification}.</li>
 * </ul>
 * The monitor is constructed with the pool as its only argument. All configuration getters
 * and setters delegate directly to the pool's {@link org.smallmind.quorum.pool.complex.ComplexPoolConfig}.
 */
public class ComponentPoolMonitor extends NotificationBroadcasterSupport implements ComponentPoolMonitorMXBean, MBeanRegistration, ComponentPoolEventListener {

  private final ComponentPool<?> componentPool;
  private ObjectName objectName;

  /**
   * Creates the monitor for the given pool and declares the two notification types it may
   * emit.
   *
   * @param componentPool the pool to expose and listen to via JMX
   */
  public ComponentPoolMonitor (ComponentPool<?> componentPool) {

    super(new MBeanNotificationInfo(new String[] {CreationErrorOccurredNotification.TYPE}, CreationErrorOccurredNotification.class.getName(), "Creation Error Occurred"), new MBeanNotificationInfo(new String[] {ComponentLeaseTimeNotification.TYPE}, ComponentLeaseTimeNotification.class.getName(), "Component Lease Time"));

    this.componentPool = componentPool;
  }

  /**
   * Called by the MBean server before registering this bean. Attaches the monitor as a pool
   * event listener and records the proposed {@link ObjectName} for use as the notification
   * source.
   *
   * @param mBeanServer the server performing the registration
   * @param objectName  the proposed name for this MBean
   * @return the name to use for registration (returned unchanged)
   */
  @Override
  public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName) {

    componentPool.addComponentPoolEventListener(this);

    return this.objectName = objectName;
  }

  /**
   * Called by the MBean server after registration. No-op.
   *
   * @param success {@code true} if registration succeeded
   */
  @Override
  public void postRegister (Boolean success) {

  }

  /**
   * Called by the MBean server before deregistering this bean. Detaches the monitor from
   * the pool's event listener list so notifications stop.
   */
  @Override
  public void preDeregister () {

    componentPool.removeComponentPoolEventListener(this);
  }

  /**
   * Called by the MBean server after deregistration. No-op.
   */
  @Override
  public void postDeregister () {

  }

  /**
   * Receives a pool error event and emits a {@link CreationErrorOccurredNotification} to
   * all JMX notification subscribers.
   *
   * @param event the error event carrying the exception
   */
  @Override
  public void reportErrorOccurred (ErrorReportingComponentPoolEvent<?> event) {

    sendNotification(new CreationErrorOccurredNotification(objectName, event.getException()));
  }

  /**
   * Receives a lease-time event and emits a {@link ComponentLeaseTimeNotification} to all
   * JMX notification subscribers.
   *
   * @param event the lease-time event carrying the duration in nanoseconds
   */
  @Override
  public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent<?> event) {

    sendNotification(new ComponentLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
  }

  /**
   * Returns the name of the monitored pool.
   *
   * @return the pool name
   */
  @Override
  public String getPoolName () {

    return componentPool.getPoolName();
  }

  /**
   * Starts the underlying pool.
   *
   * @throws ComponentPoolException if the startup sequence fails
   */
  @Override
  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  /**
   * Shuts down the underlying pool.
   *
   * @throws ComponentPoolException if the shutdown sequence fails
   */
  @Override
  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }

  /**
   * Returns whether validate-on-create is enabled.
   *
   * @return {@code true} if components are validated after creation
   */
  @Override
  public boolean isTestOnCreate () {

    return componentPool.getComplexPoolConfig().isTestOnCreate();
  }

  /**
   * Enables or disables validate-on-create.
   *
   * @param testOnCreate {@code true} to validate components after creation
   */
  @Override
  public void setTestOnCreate (boolean testOnCreate) {

    componentPool.getComplexPoolConfig().setTestOnCreate(testOnCreate);
  }

  /**
   * Returns whether validate-on-acquire is enabled.
   *
   * @return {@code true} if components are validated before being handed to a caller
   */
  @Override
  public boolean isTestOnAcquire () {

    return componentPool.getComplexPoolConfig().isTestOnAcquire();
  }

  /**
   * Enables or disables validate-on-acquire.
   *
   * @param testOnAcquire {@code true} to validate components at acquisition time
   */
  @Override
  public void setTestOnAcquire (boolean testOnAcquire) {

    componentPool.getComplexPoolConfig().setTestOnAcquire(testOnAcquire);
  }

  /**
   * Returns whether per-component lease-time reporting is enabled.
   *
   * @return {@code true} if lease-time events and metrics are emitted on return
   */
  @Override
  public boolean isReportLeaseTimeNanos () {

    return componentPool.getComplexPoolConfig().isReportLeaseTimeNanos();
  }

  /**
   * Enables or disables per-component lease-time reporting.
   *
   * @param reportLeaseTimeNanos {@code true} to emit lease-time events on each return
   */
  @Override
  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    componentPool.getComplexPoolConfig().setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  /**
   * Returns whether existential stack-trace capture is enabled.
   *
   * @return {@code true} if the acquiring thread's stack trace is recorded per component
   */
  @Override
  public boolean isExistentiallyAware () {

    return componentPool.getComplexPoolConfig().isExistentiallyAware();
  }

  /**
   * Enables or disables existential stack-trace capture.
   *
   * @param existentiallyAware {@code true} to record the acquiring thread's stack trace
   */
  @Override
  public void setExistentiallyAware (boolean existentiallyAware) {

    componentPool.getComplexPoolConfig().setExistentiallyAware(existentiallyAware);
  }

  /**
   * Returns the creation timeout in milliseconds.
   *
   * @return the creation timeout; {@code 0} for no limit
   */
  @Override
  public long getCreationTimeoutMillis () {

    return componentPool.getComplexPoolConfig().getCreationTimeoutMillis();
  }

  /**
   * Sets the creation timeout in milliseconds.
   *
   * @param creationTimeoutMillis timeout; {@code 0} for no limit
   */
  @Override
  public void setCreationTimeoutMillis (long creationTimeoutMillis) {

    componentPool.getComplexPoolConfig().setCreationTimeoutMillis(creationTimeoutMillis);
  }

  /**
   * Returns the initial pool size.
   *
   * @return the number of components pre-created at startup
   */
  @Override
  public int getInitialPoolSize () {

    return componentPool.getComplexPoolConfig().getInitialPoolSize();
  }

  /**
   * Returns the minimum pool size floor.
   *
   * @return the minimum number of components to maintain
   */
  @Override
  public int getMinPoolSize () {

    return componentPool.getComplexPoolConfig().getMinPoolSize();
  }

  /**
   * Sets the minimum pool size floor.
   *
   * @param minPoolSize minimum instances to keep alive
   */
  @Override
  public void setMinPoolSize (int minPoolSize) {

    componentPool.getComplexPoolConfig().setMinPoolSize(minPoolSize);
  }

  /**
   * Returns the maximum pool size cap.
   *
   * @return the maximum number of managed instances; {@code 0} for unbounded
   */
  @Override
  public int getMaxPoolSize () {

    return componentPool.getComplexPoolConfig().getMaxPoolSize();
  }

  /**
   * Sets the maximum pool size cap.
   *
   * @param maxPoolSize maximum instances; {@code 0} for unbounded
   */
  @Override
  public void setMaxPoolSize (int maxPoolSize) {

    componentPool.getComplexPoolConfig().setMaxPoolSize(maxPoolSize);
  }

  /**
   * Returns the acquire wait time in milliseconds.
   *
   * @return the maximum time a caller may block waiting for a component; {@code 0} for
   * immediate failure
   */
  @Override
  public synchronized long getAcquireWaitTimeMillis () {

    return componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
  }

  /**
   * Sets the acquire wait time in milliseconds.
   *
   * @param acquireWaitTimeMillis wait time; {@code 0} for immediate failure
   */
  @Override
  public synchronized void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    componentPool.getComplexPoolConfig().setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  /**
   * Returns the maximum lease time in seconds.
   *
   * @return the lease timeout; {@code 0} means no limit
   */
  @Override
  public int getMaxLeaseTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds();
  }

  /**
   * Sets the maximum lease time in seconds.
   *
   * @param leaseTimeSeconds lease timeout; {@code 0} to disable
   */
  @Override
  public void setMaxLeaseTimeSeconds (int leaseTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxLeaseTimeSeconds(leaseTimeSeconds);
  }

  /**
   * Returns the maximum idle time in seconds.
   *
   * @return the idle timeout; {@code 0} means no limit
   */
  @Override
  public int getMaxIdleTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds();
  }

  /**
   * Sets the maximum idle time in seconds.
   *
   * @param maxIdleTimeSeconds idle timeout; {@code 0} to disable
   */
  @Override
  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  /**
   * Returns the maximum processing time in seconds.
   *
   * @return the processing timeout; {@code 0} means no limit
   */
  @Override
  public int getMaxProcessingTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxProcessingTimeSeconds();
  }

  /**
   * Sets the maximum processing time in seconds.
   *
   * @param maxProcessingTimeSeconds processing timeout; {@code 0} to disable
   */
  @Override
  public void setMaxProcessingTimeSeconds (int maxProcessingTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxProcessingTimeSeconds(maxProcessingTimeSeconds);
  }

  /**
   * Returns the total number of component instances managed by the pool.
   *
   * @return the pool size (free + processing)
   */
  @Override
  public int getPoolSize () {

    return componentPool.getPoolSize();
  }

  /**
   * Returns the number of idle component instances on the free queue.
   *
   * @return the free count
   */
  @Override
  public int getFreeSize () {

    return componentPool.getFreeSize();
  }

  /**
   * Returns the number of component instances currently checked out by callers.
   *
   * @return the processing count
   */
  @Override
  public int getProcessingSize () {

    return componentPool.getProcessingSize();
  }
}

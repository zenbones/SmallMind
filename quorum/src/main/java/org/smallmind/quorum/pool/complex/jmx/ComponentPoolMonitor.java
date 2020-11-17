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

public class ComponentPoolMonitor extends NotificationBroadcasterSupport implements ComponentPoolMonitorMXBean, MBeanRegistration, ComponentPoolEventListener {

  private final ComponentPool<?> componentPool;
  private ObjectName objectName;

  public ComponentPoolMonitor (ComponentPool<?> componentPool) {

    super(new MBeanNotificationInfo(new String[] {CreationErrorOccurredNotification.TYPE}, CreationErrorOccurredNotification.class.getName(), "Creation Error Occurred"), new MBeanNotificationInfo(new String[] {ComponentLeaseTimeNotification.TYPE}, ComponentLeaseTimeNotification.class.getName(), "Component Lease Time"));

    this.componentPool = componentPool;
  }

  public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName) {

    componentPool.addComponentPoolEventListener(this);

    return this.objectName = objectName;
  }

  public void postRegister (Boolean success) {

  }

  public void preDeregister () {

    componentPool.removeComponentPoolEventListener(this);
  }

  public void postDeregister () {

  }

  @Override
  public void reportErrorOccurred (ErrorReportingComponentPoolEvent<?> event) {

    sendNotification(new CreationErrorOccurredNotification(objectName, event.getException()));
  }

  @Override
  public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent<?> event) {

    sendNotification(new ComponentLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
  }

  public String getPoolName () {

    return componentPool.getPoolName();
  }

  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }

  public boolean isTestOnCreate () {

    return componentPool.getComplexPoolConfig().isTestOnCreate();
  }

  public void setTestOnCreate (boolean testOnCreate) {

    componentPool.getComplexPoolConfig().setTestOnCreate(testOnCreate);
  }

  public boolean isTestOnAcquire () {

    return componentPool.getComplexPoolConfig().isTestOnAcquire();
  }

  public void setTestOnAcquire (boolean testOnAcquire) {

    componentPool.getComplexPoolConfig().setTestOnAcquire(testOnAcquire);
  }

  public boolean isReportLeaseTimeNanos () {

    return componentPool.getComplexPoolConfig().isReportLeaseTimeNanos();
  }

  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    componentPool.getComplexPoolConfig().setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  public boolean isExistentiallyAware () {

    return componentPool.getComplexPoolConfig().isExistentiallyAware();
  }

  public void setExistentiallyAware (boolean existentiallyAware) {

    componentPool.getComplexPoolConfig().setExistentiallyAware(existentiallyAware);
  }

  public long getCreationTimeoutMillis () {

    return componentPool.getComplexPoolConfig().getCreationTimeoutMillis();
  }

  public void setCreationTimeoutMillis (long creationTimeoutMillis) {

    componentPool.getComplexPoolConfig().setCreationTimeoutMillis(creationTimeoutMillis);
  }

  public int getInitialPoolSize () {

    return componentPool.getComplexPoolConfig().getInitialPoolSize();
  }

  public int getMinPoolSize () {

    return componentPool.getComplexPoolConfig().getMinPoolSize();
  }

  public void setMinPoolSize (int minPoolSize) {

    componentPool.getComplexPoolConfig().setMinPoolSize(minPoolSize);
  }

  public int getMaxPoolSize () {

    return componentPool.getComplexPoolConfig().getMaxPoolSize();
  }

  public void setMaxPoolSize (int maxPoolSize) {

    componentPool.getComplexPoolConfig().setMaxPoolSize(maxPoolSize);
  }

  public synchronized long getAcquireWaitTimeMillis () {

    return componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
  }

  public synchronized void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    componentPool.getComplexPoolConfig().setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  public int getMaxLeaseTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds();
  }

  public void setMaxLeaseTimeSeconds (int leaseTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxLeaseTimeSeconds(leaseTimeSeconds);
  }

  public int getMaxIdleTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds();
  }

  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  public int getUnReturnedElementTimeoutSeconds () {

    return componentPool.getComplexPoolConfig().getUnReturnedElementTimeoutSeconds();
  }

  public void setUnReturnedElementTimeoutSeconds (int unReturnedElementTimeoutSeconds) {

    componentPool.getComplexPoolConfig().setUnReturnedElementTimeoutSeconds(unReturnedElementTimeoutSeconds);
  }

  public int getPoolSize () {

    return componentPool.getPoolSize();
  }

  public int getFreeSize () {

    return componentPool.getFreeSize();
  }

  public int getProcessingSize () {

    return componentPool.getProcessingSize();
  }
}

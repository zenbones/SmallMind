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
package org.smallmind.quorum.pool.complex.remote;

import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.quorum.pool.complex.ComponentPoolException;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.transport.remote.RemoteEndpoint;

public class RemoteComponentPoolSurfaceImpl implements RemoteComponentPoolSurface, RemoteEndpoint {

  private static final Class[] REMOTE_INTERFACES = new Class[] {RemoteComponentPoolSurface.class};

  private final ComponentPool<?> componentPool;

  public RemoteComponentPoolSurfaceImpl (ComponentPool<?> componentPool) {

    this.componentPool = componentPool;
  }

  public Class[] getProxyInterfaces () {

    return REMOTE_INTERFACES;
  }

  @Override
  public void addComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPool.addComponentPoolEventListener(listener);
  }

  @Override
  public void removeComponentPoolEventListener (ComponentPoolEventListener listener) {

    componentPool.removeComponentPoolEventListener(listener);
  }

  @Override
  public String getPoolName () {

    return componentPool.getPoolName();
  }

  @Override
  public void startup ()
    throws ComponentPoolException {

    componentPool.startup();
  }

  @Override
  public void shutdown ()
    throws ComponentPoolException {

    componentPool.shutdown();
  }

  @Override
  public int getPoolSize () {

    return componentPool.getPoolSize();
  }

  @Override
  public int getFreeSize () {

    return componentPool.getFreeSize();
  }

  @Override
  public int getProcessingSize () {

    return componentPool.getProcessingSize();
  }

  @Override
  public boolean isTestOnConnect () {

    return componentPool.getComplexPoolConfig().getTestOnCreate();
  }

  @Override
  public void setTestOnConnect (boolean testOnConnect) {

    componentPool.getComplexPoolConfig().setTestOnCreate(testOnConnect);
  }

  @Override
  public boolean isTestOnAcquire () {

    return componentPool.getComplexPoolConfig().isTestOnAcquire();
  }

  @Override
  public void setTestOnAcquire (boolean testOnAcquire) {

    componentPool.getComplexPoolConfig().setTestOnAcquire(testOnAcquire);
  }

  @Override
  public boolean isReportLeaseTimeNanos () {

    return componentPool.getComplexPoolConfig().isReportLeaseTimeNanos();
  }

  @Override
  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    componentPool.getComplexPoolConfig().setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  @Override
  public boolean isExistentiallyAware () {

    return componentPool.getComplexPoolConfig().isExistentiallyAware();
  }

  @Override
  public void setExistentiallyAware (boolean existentiallyAware) {

    componentPool.getComplexPoolConfig().setExistentiallyAware(existentiallyAware);
  }

  @Override
  public long getConnectionTimeoutMillis () {

    return componentPool.getComplexPoolConfig().getElementCreationTimeoutMillis();
  }

  @Override
  public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    componentPool.getComplexPoolConfig().setElementCreationTimeoutMillis(connectionTimeoutMillis);
  }

  @Override
  public long getAcquireWaitTimeMillis () {

    return componentPool.getComplexPoolConfig().getAcquireWaitTimeMillis();
  }

  @Override
  public void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    componentPool.getComplexPoolConfig().setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  @Override
  public int getInitialPoolSize () {

    return componentPool.getComplexPoolConfig().getInitialPoolSize();
  }

  @Override
  public int getMinPoolSize () {

    return componentPool.getComplexPoolConfig().getMinPoolSize();
  }

  @Override
  public void setMinPoolSize (int minPoolSize) {

    componentPool.getComplexPoolConfig().setMinPoolSize(minPoolSize);
  }

  @Override
  public int getMaxPoolSize () {

    return componentPool.getComplexPoolConfig().getMaxPoolSize();
  }

  @Override
  public void setMaxPoolSize (int maxPoolSize) {

    componentPool.getComplexPoolConfig().setMaxPoolSize(maxPoolSize);
  }

  @Override
  public int getMaxLeaseTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxLeaseTimeSeconds();
  }

  @Override
  public void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxLeaseTimeSeconds(maxLeaseTimeSeconds);
  }

  @Override
  public int getMaxIdleTimeSeconds () {

    return componentPool.getComplexPoolConfig().getMaxIdleTimeSeconds();
  }

  @Override
  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    componentPool.getComplexPoolConfig().setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  @Override
  public int getUnReturnedElementTimeoutSeconds () {

    return componentPool.getComplexPoolConfig().getUnReturnedElementTimeoutSeconds();
  }

  @Override
  public void setUnReturnedElementTimeoutSeconds (int unReturnedElementTimeoutSeconds) {

    componentPool.getComplexPoolConfig().setUnReturnedElementTimeoutSeconds(unReturnedElementTimeoutSeconds);
  }
}

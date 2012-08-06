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
package org.smallmind.quorum.pool.complex.jmx;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.management.StandardEmitterMBean;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.complex.ComponentPoolException;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolSurface;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;

public class ComponentPoolMonitors extends StandardEmitterMBean implements ComponentPoolMonitorsMBean, ComponentPoolEventListener {

  private static final String REGISTRY_NAME = ComponentPoolMonitors.class.getPackage().getName() + ".listener";

  private final ConcurrentHashMap<String, RemoteComponentPoolSurface> handleMap = new ConcurrentHashMap<String, RemoteComponentPoolSurface>();

  private ObjectName objectName;
  private ComponentPoolEventListener remoteListener;

  public ComponentPoolMonitors () {

    super(ComponentPoolMonitorsMBean.class, false, new NotificationBroadcasterSupport(new MBeanNotificationInfo(new String[] {ConnectionErrorOccurredNotification.TYPE}, ConnectionErrorOccurredNotification.class.getName(), "Connection Error Occurred"), new MBeanNotificationInfo(new String[] {ConnectionLeaseTimeNotification.TYPE}, ConnectionLeaseTimeNotification.class.getName(), "Connection Lease Time")));
  }

  public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

    RemoteEndpointBinder.bind(new RemoteComponentPoolEventListener(this), REGISTRY_NAME);
    remoteListener = RemoteProxyFactory.generateRemoteProxy(ComponentPoolEventListener.class, REGISTRY_NAME);

    return this.objectName = objectName;
  }

  public void postRegister (Boolean success) {

  }

  public void preDeregister ()
    throws MalformedURLException, NotBoundException, RemoteException {

    for (RemoteComponentPoolSurface remoteSurface : handleMap.values()) {
      remoteSurface.removeComponentPoolEventListener(remoteListener);
    }

    handleMap.clear();
    RemoteEndpointBinder.unbind(REGISTRY_NAME);
  }

  public void postDeregister () {

  }

  public void reportErrorOccurred (ErrorReportingComponentPoolEvent event) {

    sendNotification(new ConnectionErrorOccurredNotification(objectName, event.getException()));
  }

  public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent event) {

    sendNotification(new ConnectionLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
  }

  private RemoteComponentPoolSurface getRemoteSurface (String poolId)
    throws ComponentPoolRegistrationException {

    RemoteComponentPoolSurface remoteSurface;

    if ((remoteSurface = handleMap.get(poolId)) == null) {
      throw new ComponentPoolRegistrationException("Attempt to access an unregistered pool(%s)", poolId);
    }

    return remoteSurface;
  }

  public synchronized void registerComponentPool (String poolId, RemoteComponentPoolSurface remoteSurface)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

    RemoteComponentPoolSurface priorRemoteSurface;

    if ((priorRemoteSurface = handleMap.put(poolId, remoteSurface)) != null) {
      priorRemoteSurface.removeComponentPoolEventListener(remoteListener);
    }

    remoteSurface.addComponentPoolEventListener(remoteListener);
  }

  public synchronized void removeComponentPool (String poolId)
    throws ComponentPoolRegistrationException, MalformedURLException, NotBoundException, RemoteException {

    RemoteComponentPoolSurface remoteSurface;

    if ((remoteSurface = handleMap.remove(poolId)) == null) {
      throw new ComponentPoolRegistrationException("Attempt to access an unregistered pool(%s)", poolId);
    }

    remoteSurface.removeComponentPoolEventListener(remoteListener);
  }

  public String getPoolName (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getPoolName();
  }

  public void startup (String poolId)
    throws ComponentPoolRegistrationException, ComponentPoolException {

    getRemoteSurface(poolId).startup();
  }

  public void shutdown (String poolId)
    throws ComponentPoolRegistrationException, ComponentPoolException {

    getRemoteSurface(poolId).shutdown();
  }

  public int getPoolSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getPoolSize();
  }

  public int getFreeSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getFreeSize();
  }

  public int getProcessingSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getProcessingSize();
  }

  public boolean isTestOnConnect (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).isTestOnConnect();
  }

  public void setTestOnConnect (String poolId, boolean testOnConnect)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setTestOnConnect(testOnConnect);
  }

  public boolean isTestOnAcquire (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).isTestOnAcquire();
  }

  public void setTestOnAcquire (String poolId, boolean testOnAcquire)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setTestOnAcquire(testOnAcquire);
  }

  public boolean isReportLeaseTimeNanos (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).isReportLeaseTimeNanos();
  }

  public void setReportLeaseTimeNanos (String poolId, boolean reportLeaseTimeNanos)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  public boolean isExistentiallyAware (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).isExistentiallyAware();
  }

  public void setExistentiallyAware (String poolId, boolean existentiallyAware)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setExistentiallyAware(existentiallyAware);
  }

  public long getConnectionTimeoutMillis (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getConnectionTimeoutMillis();
  }

  public void setConnectionTimeoutMillis (String poolId, long connectionTimeoutMillis)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setConnectionTimeoutMillis(connectionTimeoutMillis);
  }

  public int getInitialPoolSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getInitialPoolSize();
  }

  public int getMinPoolSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getMinPoolSize();
  }

  public void setMinPoolSize (String poolId, int minPoolSize)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setMinPoolSize(minPoolSize);
  }

  public int getMaxPoolSize (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getMaxPoolSize();
  }

  public void setMaxPoolSize (String poolId, int maxPoolSize)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setMaxPoolSize(maxPoolSize);
  }

  public long getAcquireWaitTimeMillis (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getAcquireWaitTimeMillis();
  }

  public void setAcquireWaitTimeMillis (String poolId, long acquireWaitTimeMillis)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  public int getMaxLeaseTimeSeconds (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getMaxLeaseTimeSeconds();
  }

  public void setMaxLeaseTimeSeconds (String poolId, int leaseTimeSeconds)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setMaxLeaseTimeSeconds(leaseTimeSeconds);
  }

  public int getMaxIdleTimeSeconds (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getMaxIdleTimeSeconds();
  }

  public void setMaxIdleTimeSeconds (String poolId, int maxIdleTimeSeconds)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  public int getUnReturnedElementTimeoutSeconds (String poolId)
    throws ComponentPoolRegistrationException {

    return getRemoteSurface(poolId).getUnReturnedElementTimeoutSeconds();
  }

  public void setUnReturnedElementTimeoutSeconds (String poolId, int unReturnedElementTimeoutSeconds)
    throws ComponentPoolRegistrationException {

    getRemoteSurface(poolId).setUnReturnedElementTimeoutSeconds(unReturnedElementTimeoutSeconds);
  }
}

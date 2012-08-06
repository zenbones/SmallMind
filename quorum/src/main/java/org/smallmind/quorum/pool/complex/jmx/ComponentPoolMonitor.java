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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.complex.ComponentPool;
import org.smallmind.quorum.pool.complex.ComponentPoolException;
import org.smallmind.quorum.pool.complex.event.ComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.event.ErrorReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.event.LeaseTimeReportingComponentPoolEvent;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolEventListener;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolSurface;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolSurfaceImpl;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;

public class ComponentPoolMonitor extends NotificationBroadcasterSupport implements ComponentPoolMonitorMXBean, MBeanRegistration, ComponentPoolEventListener {

  private ObjectName objectName;
  private RemoteComponentPoolSurface remoteSurface;
  private ComponentPoolEventListener remoteListener;
  private String registryName;

  public ComponentPoolMonitor (ComponentPool componentPool, String registryName)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

    this(componentPool, InetAddress.getLocalHost().getHostAddress(), registryName);
  }

  public ComponentPoolMonitor (ComponentPool componentPool, String hostName, String registryName)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

    super(new MBeanNotificationInfo(new String[] {ConnectionErrorOccurredNotification.TYPE}, ConnectionErrorOccurredNotification.class.getName(), "Connection Error Occurred"), new MBeanNotificationInfo(new String[] {ConnectionLeaseTimeNotification.TYPE}, ConnectionLeaseTimeNotification.class.getName(), "Connection Lease Time"));

    this.registryName = registryName;

    RemoteEndpointBinder.bind(new RemoteComponentPoolSurfaceImpl(componentPool), registryName);
    remoteSurface = RemoteProxyFactory.generateRemoteProxy(RemoteComponentPoolSurface.class, hostName, registryName);
  }

  public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

    RemoteEndpointBinder.bind(new RemoteComponentPoolEventListener(this), registryName + ".listener");
    remoteSurface.addComponentPoolEventListener(remoteListener = RemoteProxyFactory.generateRemoteProxy(ComponentPoolEventListener.class, registryName + ".listener"));

    return this.objectName = objectName;
  }

  public void postRegister (Boolean success) {

  }

  public void preDeregister ()
    throws MalformedURLException, NotBoundException, RemoteException {

    remoteSurface.removeComponentPoolEventListener(remoteListener);
    RemoteEndpointBinder.unbind(registryName + ".listener");
  }

  public void postDeregister () {

  }

  public void reportErrorOccurred (ErrorReportingComponentPoolEvent event) {

    sendNotification(new ConnectionErrorOccurredNotification(objectName, event.getException()));
  }

  public void reportLeaseTime (LeaseTimeReportingComponentPoolEvent event) {

    sendNotification(new ConnectionLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
  }

  public String getPoolName () {

    return remoteSurface.getPoolName();
  }

  public void startup ()
    throws ComponentPoolException {

    remoteSurface.startup();
  }

  public void shutdown ()
    throws ComponentPoolException {

    remoteSurface.shutdown();
  }

  public boolean isTestOnConnect () {

    return remoteSurface.isTestOnConnect();
  }

  public void setTestOnConnect (boolean testOnConnect) {

    remoteSurface.setTestOnConnect(testOnConnect);
  }

  public boolean isTestOnAcquire () {

    return remoteSurface.isTestOnAcquire();
  }

  public void setTestOnAcquire (boolean testOnAcquire) {

    remoteSurface.setTestOnAcquire(testOnAcquire);
  }

  public boolean isReportLeaseTimeNanos () {

    return remoteSurface.isReportLeaseTimeNanos();
  }

  public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {

    remoteSurface.setReportLeaseTimeNanos(reportLeaseTimeNanos);
  }

  public boolean isExistentiallyAware () {

    return remoteSurface.isExistentiallyAware();
  }

  public void setExistentiallyAware (boolean existentiallyAware) {

    remoteSurface.setExistentiallyAware(existentiallyAware);
  }

  public long getConnectionTimeoutMillis () {

    return remoteSurface.getConnectionTimeoutMillis();
  }

  public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

    remoteSurface.setConnectionTimeoutMillis(connectionTimeoutMillis);
  }

  public int getInitialPoolSize () {

    return remoteSurface.getInitialPoolSize();
  }

  public int getMinPoolSize () {

    return remoteSurface.getMinPoolSize();
  }

  public void setMinPoolSize (int minPoolSize) {

    remoteSurface.setMinPoolSize(minPoolSize);
  }

  public int getMaxPoolSize () {

    return remoteSurface.getMaxPoolSize();
  }

  public void setMaxPoolSize (int maxPoolSize) {

    remoteSurface.setMaxPoolSize(maxPoolSize);
  }

  public synchronized long getAcquireWaitTimeMillis () {

    return remoteSurface.getAcquireWaitTimeMillis();
  }

  public synchronized void setAcquireWaitTimeMillis (long acquireWaitTimeMillis) {

    remoteSurface.setAcquireWaitTimeMillis(acquireWaitTimeMillis);
  }

  public int getMaxLeaseTimeSeconds () {

    return remoteSurface.getMaxLeaseTimeSeconds();
  }

  public void setMaxLeaseTimeSeconds (int leaseTimeSeconds) {

    remoteSurface.setMaxLeaseTimeSeconds(leaseTimeSeconds);
  }

  public int getMaxIdleTimeSeconds () {

    return remoteSurface.getMaxIdleTimeSeconds();
  }

  public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {

    remoteSurface.setMaxIdleTimeSeconds(maxIdleTimeSeconds);
  }

  public int getUnReturnedElementTimeoutSeconds () {

    return remoteSurface.getUnReturnedElementTimeoutSeconds();
  }

  public void setUnReturnedElementTimeoutSeconds (int unReturnedElementTimeoutSeconds) {

    remoteSurface.setUnReturnedElementTimeoutSeconds(unReturnedElementTimeoutSeconds);
  }

  public int getPoolSize () {

    return remoteSurface.getPoolSize();
  }

  public int getFreeSize () {

    return remoteSurface.getFreeSize();
  }

  public int getProcessingSize () {

    return remoteSurface.getProcessingSize();
  }
}

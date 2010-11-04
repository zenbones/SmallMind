/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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
package org.smallmind.quorum.pool.jmx;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.quorum.pool.event.ConnectionPoolEventListener;
import org.smallmind.quorum.pool.event.ErrorReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.event.LeaseTimeReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolEventListener;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolSurface;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;

public class ConnectionPoolMonitor extends NotificationBroadcasterSupport implements ConnectionPoolMonitorMXBean, MBeanRegistration, ConnectionPoolEventListener {

   private ObjectName objectName;
   private RemoteConnectionPoolSurface remoteSurface;
   private RemoteConnectionPoolEventListener remoteListener;
   private String registryName;

   public ConnectionPoolMonitor (ConnectionPool connectionPool, String registryName)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      this(connectionPool, InetAddress.getLocalHost().getHostAddress(), registryName);
   }

   public ConnectionPoolMonitor (ConnectionPool connectionPool, String hostName, String registryName)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      super(new MBeanNotificationInfo(new String[] {ConnectionErrorOccurredNotification.TYPE}, ConnectionErrorOccurredNotification.class.getName(), "Connection Error Occurred"), new MBeanNotificationInfo(new String[] {ConnectionLeaseTimeNotification.TYPE}, ConnectionLeaseTimeNotification.class.getName(), "Connection Lease Time"));

      this.registryName = registryName;

      RemoteEndpointBinder.bind(connectionPool, registryName);
      remoteSurface = RemoteProxyFactory.generateRemoteProxy(RemoteConnectionPoolSurface.class, hostName, registryName);
   }

   public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      remoteListener = new RemoteConnectionPoolEventListener(this);
      RemoteEndpointBinder.bind(remoteListener, registryName + ".listener");

      remoteSurface.addConnectionPoolEventListener(RemoteProxyFactory.generateRemoteProxy(ConnectionPoolEventListener.class, registryName + ".listener"));

      return this.objectName = objectName;
   }

   public void postRegister (Boolean success) {
   }

   public void preDeregister () {

      remoteSurface.removeConnectionPoolEventListener(remoteListener);
   }

   public void postDeregister () {
   }

   public void connectionErrorOccurred (ErrorReportingConnectionPoolEvent event) {

      sendNotification(new ConnectionErrorOccurredNotification(objectName, event.getException()));
   }

   public void connectionLeaseTime (LeaseTimeReportingConnectionPoolEvent event) {

      sendNotification(new ConnectionLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
   }

   public String getPoolName () {

      return remoteSurface.getPoolName();
   }

   public PoolMode getPoolMode () {

      return remoteSurface.getPoolMode();
   }

   public void setPoolMode (PoolMode poolMode) {

      remoteSurface.setPoolMode(poolMode);
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

   public long getConnectionTimeoutMillis () {

      return remoteSurface.getConnectionTimeoutMillis();
   }

   public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {

      remoteSurface.setConnectionTimeoutMillis(connectionTimeoutMillis);
   }

   public int getInitialPoolSize () {

      return remoteSurface.getPoolSize();
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

   public int getAcquireRetryAttempts () {

      return remoteSurface.getAcquireRetryAttempts();
   }

   public void setAcquireRetryAttempts (int acquireRetryAttempts) {

      remoteSurface.setAcquireRetryAttempts(acquireRetryAttempts);
   }

   public int getAcquireRetryDelayMillis () {

      return remoteSurface.getAcquireRetryDelayMillis();
   }

   public void setAcquireRetryDelayMillis (int acquireRetryDelayMillis) {

      remoteSurface.setAcquireRetryDelayMillis(acquireRetryDelayMillis);
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

   public int getUnreturnedConnectionTimeoutSeconds () {

      return remoteSurface.getUnreturnedConnectionTimeoutSeconds();
   }

   public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {

      remoteSurface.setUnreturnedConnectionTimeoutSeconds(unreturnedConnectionTimeoutSeconds);
   }

   public void startup ()
      throws ConnectionPoolException {

      remoteSurface.startup();
   }

   public void shutdown () {

      remoteSurface.shutdown();
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

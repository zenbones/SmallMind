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
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.quorum.pool.event.ConnectionPoolEventListener;
import org.smallmind.quorum.pool.event.ErrorReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.event.LeaseTimeReportingConnectionPoolEvent;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolEventListener;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolSurface;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;

public class ConnectionPoolMonitors extends StandardEmitterMBean implements ConnectionPoolMonitorsMBean, ConnectionPoolEventListener {

   private static final String REGISTRY_NAME = ConnectionPoolMonitors.class.getPackage().getName() + ".listener";

   private final ConcurrentHashMap<String, RemoteConnectionPoolSurface> handleMap = new ConcurrentHashMap<String, RemoteConnectionPoolSurface>();

   private ObjectName objectName;
   private ConnectionPoolEventListener remoteListener;

   public ConnectionPoolMonitors () {

      super(ConnectionPoolMonitorsMBean.class, false, new NotificationBroadcasterSupport(new MBeanNotificationInfo(new String[] {ConnectionErrorOccurredNotification.TYPE}, ConnectionErrorOccurredNotification.class.getName(), "Connection Error Occurred"), new MBeanNotificationInfo(new String[] {ConnectionLeaseTimeNotification.TYPE}, ConnectionLeaseTimeNotification.class.getName(), "Connection Lease Time")));
   }

   public ObjectName preRegister (MBeanServer mBeanServer, ObjectName objectName)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      RemoteEndpointBinder.bind(new RemoteConnectionPoolEventListener(this), REGISTRY_NAME);
      remoteListener = RemoteProxyFactory.generateRemoteProxy(ConnectionPoolEventListener.class, REGISTRY_NAME);

      return this.objectName = objectName;
   }

   public void postRegister (Boolean success) {
   }

   public void preDeregister ()
      throws MalformedURLException, NotBoundException, RemoteException {

      for (RemoteConnectionPoolSurface remoteSurface : handleMap.values()) {
         remoteSurface.removeConnectionPoolEventListener(remoteListener);
      }

      handleMap.clear();
      RemoteEndpointBinder.unbind(REGISTRY_NAME);
   }

   public void postDeregister () {
   }

   public void connectionErrorOccurred (ErrorReportingConnectionPoolEvent event) {

      sendNotification(new ConnectionErrorOccurredNotification(objectName, event.getException()));
   }

   public void connectionLeaseTime (LeaseTimeReportingConnectionPoolEvent event) {

      sendNotification(new ConnectionLeaseTimeNotification(objectName, event.getLeaseTimeNanos()));
   }

   private RemoteConnectionPoolSurface getRemoteSurface (String poolId)
      throws ConnectionPoolRegistrationException {

      RemoteConnectionPoolSurface remoteSurface;

      if ((remoteSurface = handleMap.get(poolId)) == null) {
         throw new ConnectionPoolRegistrationException("Attempt to access an unregistered pool(%s)", poolId);
      }

      return remoteSurface;
   }

   public synchronized void registerConnectionPool (String poolId, RemoteConnectionPoolSurface remoteSurface)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      RemoteConnectionPoolSurface priorRemoteSurface;

      if ((priorRemoteSurface = handleMap.put(poolId, remoteSurface)) != null) {
         priorRemoteSurface.removeConnectionPoolEventListener(remoteListener);
      }

      remoteSurface.addConnectionPoolEventListener(remoteListener);
   }

   public synchronized void removeConnectionPool (String poolId)
      throws ConnectionPoolRegistrationException, MalformedURLException, NotBoundException, RemoteException {

      RemoteConnectionPoolSurface remoteSurface;

      if ((remoteSurface = handleMap.remove(poolId)) == null) {
         throw new ConnectionPoolRegistrationException("Attempt to access an unregistered pool(%s)", poolId);
      }

      remoteSurface.removeConnectionPoolEventListener(remoteListener);
   }

   public String getPoolName (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getPoolName();
   }

   public PoolMode getPoolMode (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getPoolMode();
   }

   public void setPoolMode (String poolId, PoolMode poolMode)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setPoolMode(poolMode);
   }

   public boolean isTestOnConnect (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).isTestOnConnect();
   }

   public void setTestOnConnect (String poolId, boolean testOnConnect)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setTestOnConnect(testOnConnect);
   }

   public boolean isTestOnAcquire (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).isTestOnAcquire();
   }

   public void setTestOnAcquire (String poolId, boolean testOnAcquire)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setTestOnAcquire(testOnAcquire);
   }

   public boolean isReportLeaseTimeNanos (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).isReportLeaseTimeNanos();
   }

   public void setReportLeaseTimeNanos (String poolId, boolean reportLeaseTimeNanos)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setReportLeaseTimeNanos(reportLeaseTimeNanos);
   }

   public long getConnectionTimeoutMillis (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getConnectionTimeoutMillis();
   }

   public void setConnectionTimeoutMillis (String poolId, long connectionTimeoutMillis)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setConnectionTimeoutMillis(connectionTimeoutMillis);
   }

   public int getInitialPoolSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getPoolSize();
   }

   public int getMinPoolSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getMinPoolSize();
   }

   public void setMinPoolSize (String poolId, int minPoolSize)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setMinPoolSize(minPoolSize);
   }

   public int getMaxPoolSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getMaxPoolSize();
   }

   public void setMaxPoolSize (String poolId, int maxPoolSize)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setMaxPoolSize(maxPoolSize);
   }

   public int getAcquireRetryAttempts (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getAcquireRetryAttempts();
   }

   public void setAcquireRetryAttempts (String poolId, int acquireRetryAttempts)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setAcquireRetryAttempts(acquireRetryAttempts);
   }

   public int getAcquireRetryDelayMillis (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getAcquireRetryDelayMillis();
   }

   public void setAcquireRetryDelayMillis (String poolId, int acquireRetryDelayMillis)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setAcquireRetryDelayMillis(acquireRetryDelayMillis);
   }

   public int getMaxLeaseTimeSeconds (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getMaxLeaseTimeSeconds();
   }

   public void setMaxLeaseTimeSeconds (String poolId, int leaseTimeSeconds)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setMaxLeaseTimeSeconds(leaseTimeSeconds);
   }

   public int getMaxIdleTimeSeconds (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getMaxIdleTimeSeconds();
   }

   public void setMaxIdleTimeSeconds (String poolId, int maxIdleTimeSeconds)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setMaxIdleTimeSeconds(maxIdleTimeSeconds);
   }

   public int getUnreturnedConnectionTimeoutSeconds (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getUnreturnedConnectionTimeoutSeconds();
   }

   public void setUnreturnedConnectionTimeoutSeconds (String poolId, int unreturnedConnectionTimeoutSeconds)
      throws ConnectionPoolRegistrationException {

      getRemoteSurface(poolId).setUnreturnedConnectionTimeoutSeconds(unreturnedConnectionTimeoutSeconds);
   }

   public void startup (String poolId)
      throws ConnectionPoolRegistrationException, ConnectionPoolException {

      getRemoteSurface(poolId).startup();
   }

   public void shutdown (String poolId)
      throws ConnectionPoolRegistrationException, ConnectionPoolException {

      getRemoteSurface(poolId).shutdown();
   }

   public int getPoolSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getPoolSize();
   }

   public int getFreeSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getFreeSize();
   }

   public int getProcessingSize (String poolId)
      throws ConnectionPoolRegistrationException {

      return getRemoteSurface(poolId).getProcessingSize();
   }
}

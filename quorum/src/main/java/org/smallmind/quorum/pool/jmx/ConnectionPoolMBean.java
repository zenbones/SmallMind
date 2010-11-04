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
import java.rmi.RemoteException;
import javax.naming.NamingException;
import org.smallmind.quorum.pool.ConnectionPool;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.ConnectionPoolSurface;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.quorum.pool.event.ConnectionPoolEventListener;
import org.smallmind.quorum.transport.remote.RemoteEndpointBinder;
import org.smallmind.quorum.transport.remote.RemoteProxyFactory;

public class ConnectionPoolMBean implements ConnectionPoolMXBean {

   private ConnectionPoolSurface connectionPoolSurface;

   public ConnectionPoolMBean (ConnectionPool connectionPool, String hostName, String registryName)
      throws NoSuchMethodException, MalformedURLException, RemoteException, NamingException {

      RemoteEndpointBinder.bind(connectionPool, registryName);
      connectionPoolSurface = RemoteProxyFactory.generateRemoteProxy(ConnectionPoolSurface.class, hostName, registryName);
   }

   public String getPoolName () {

      return connectionPoolSurface.getPoolName();
   }

   public PoolMode getPoolMode () {

      return connectionPoolSurface.getPoolMode();
   }

   public void setPoolMode (PoolMode poolMode) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isTestOnConnect () {

      return connectionPoolSurface.isTestOnConnect();
   }

   public void setTestOnConnect (boolean testOnConnect) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isTestOnAcquire () {

      return connectionPoolSurface.isTestOnAcquire();
   }

   public void setTestOnAcquire (boolean testOnAcquire) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public boolean isReportLeaseTimeNanos () {

      return connectionPoolSurface.isReportLeaseTimeNanos();
   }

   public void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public long getConnectionTimeoutMillis () {

      return connectionPoolSurface.getConnectionTimeoutMillis();
   }

   public void setConnectionTimeoutMillis (long connectionTimeoutMillis) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getInitialPoolSize () {

      return connectionPoolSurface.getPoolSize();
   }

   public int getMinPoolSize () {

      return connectionPoolSurface.getMinPoolSize();
   }

   public void setMinPoolSize (int minPoolSize) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getMaxPoolSize () {

      return connectionPoolSurface.getMaxPoolSize();
   }

   public void setMaxPoolSize (int maxPoolSize) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getAcquireRetryAttempts () {

      return connectionPoolSurface.getAcquireRetryAttempts();
   }

   public void setAcquireRetryAttempts (int acquireRetryAttempts) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getAcquireRetryDelayMillis () {

      return connectionPoolSurface.getAcquireRetryDelayMillis();
   }

   public void setAcquireRetryDelayMillis (int acquireRetryDelayMillis) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getMaxLeaseTimeSeconds () {

      return connectionPoolSurface.getMaxLeaseTimeSeconds();
   }

   public void setMaxLeaseTimeSeconds (int leaseTimeSeconds) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getMaxIdleTimeSeconds () {

      return connectionPoolSurface.getMaxIdleTimeSeconds();
   }

   public void setMaxIdleTimeSeconds (int maxIdleTimeSeconds) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getUnreturnedConnectionTimeoutSeconds () {

      return connectionPoolSurface.getUnreturnedConnectionTimeoutSeconds();
   }

   public void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void startup ()
      throws ConnectionPoolException {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void shutdown () {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public int getPoolSize () {

      return connectionPoolSurface.getPoolSize();
   }

   public int getFreeSize () {

      return connectionPoolSurface.getFreeSize();
   }

   public int getProcessingSize () {

      return connectionPoolSurface.getProcessingSize();
   }

   public void addConnectionPoolEventListener (ConnectionPoolEventListener listener) {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void removeConnectionPoolEventListener (ConnectionPoolEventListener listener) {
      //To change body of implemented methods use File | Settings | File Templates.
   }
}

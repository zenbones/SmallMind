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
import javax.naming.NamingException;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.PoolMode;
import org.smallmind.quorum.pool.remote.RemoteConnectionPoolSurface;

public interface ConnectionPoolMonitorsMBean {

   public abstract void registerConnectionPool (String poolId, RemoteConnectionPoolSurface remoteSurface)
      throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException;

   public abstract void removeConnectionPool (String poolId)
      throws ConnectionPoolRegistrationException, MalformedURLException, NotBoundException, RemoteException;

   public abstract String getPoolName (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract PoolMode getPoolMode (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setPoolMode (String poolId, PoolMode poolMode)
      throws ConnectionPoolRegistrationException;

   public abstract boolean isTestOnConnect (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setTestOnConnect (String poolId, boolean testOnConnect)
      throws ConnectionPoolRegistrationException;

   public abstract boolean isTestOnAcquire (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setTestOnAcquire (String poolId, boolean testOnAcquire)
      throws ConnectionPoolRegistrationException;

   public abstract boolean isReportLeaseTimeNanos (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setReportLeaseTimeNanos (String poolId, boolean reportLeaseTimeNanos)
      throws ConnectionPoolRegistrationException;

   public abstract long getConnectionTimeoutMillis (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setConnectionTimeoutMillis (String poolId, long connectionTimeoutMillis)
      throws ConnectionPoolRegistrationException;

   public abstract int getInitialPoolSize (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract int getMinPoolSize (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setMinPoolSize (String poolId, int minPoolSize)
      throws ConnectionPoolRegistrationException;

   public abstract int getMaxPoolSize (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setMaxPoolSize (String poolId, int maxPoolSize)
      throws ConnectionPoolRegistrationException;

   public abstract int getAcquireRetryAttempts (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setAcquireRetryAttempts (String poolId, int acquireRetryAttempts)
      throws ConnectionPoolRegistrationException;

   public abstract int getAcquireRetryDelayMillis (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setAcquireRetryDelayMillis (String poolId, int acquireRetryDelayMillis)
      throws ConnectionPoolRegistrationException;

   public abstract int getMaxLeaseTimeSeconds (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setMaxLeaseTimeSeconds (String poolId, int maxLeaseTimeSeconds)
      throws ConnectionPoolRegistrationException;

   public abstract int getMaxIdleTimeSeconds (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setMaxIdleTimeSeconds (String poolId, int maxIdleTimeSeconds)
      throws ConnectionPoolRegistrationException;

   public abstract int getUnreturnedConnectionTimeoutSeconds (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract void setUnreturnedConnectionTimeoutSeconds (String poolId, int unreturnedConnectionTimeoutSeconds)
      throws ConnectionPoolRegistrationException;

   public abstract void startup (String poolId)
      throws ConnectionPoolException, ConnectionPoolRegistrationException;

   public abstract void shutdown (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract int getPoolSize (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract int getFreeSize (String poolId)
      throws ConnectionPoolRegistrationException;

   public abstract int getProcessingSize (String poolId)
      throws ConnectionPoolRegistrationException;
}

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
import javax.naming.NamingException;
import org.smallmind.quorum.pool.complex.ComponentPoolException;
import org.smallmind.quorum.pool.complex.remote.RemoteComponentPoolSurface;

public interface ComponentPoolMonitorsMBean {

  public abstract void registerComponentPool (String poolId, RemoteComponentPoolSurface remoteSurface)
    throws UnknownHostException, NoSuchMethodException, MalformedURLException, RemoteException, NamingException;

  public abstract void removeComponentPool (String poolId)
    throws ComponentPoolRegistrationException, MalformedURLException, NotBoundException, RemoteException;

  public abstract String getPoolName (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void startup (String poolId)
    throws ComponentPoolException, ComponentPoolRegistrationException;

  public abstract void shutdown (String poolId)
    throws ComponentPoolException, ComponentPoolRegistrationException;

  public abstract int getPoolSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract int getFreeSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract int getProcessingSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract boolean isTestOnConnect (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setTestOnConnect (String poolId, boolean testOnConnect)
    throws ComponentPoolRegistrationException;

  public abstract boolean isTestOnAcquire (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setTestOnAcquire (String poolId, boolean testOnAcquire)
    throws ComponentPoolRegistrationException;

  public abstract boolean isReportLeaseTimeNanos (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setReportLeaseTimeNanos (String poolId, boolean reportLeaseTimeNanos)
    throws ComponentPoolRegistrationException;

  public abstract boolean isExistentiallyAware (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setExistentiallyAware (String poolId, boolean existentiallyAware)
    throws ComponentPoolRegistrationException;

  public abstract long getConnectionTimeoutMillis (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setConnectionTimeoutMillis (String poolId, long connectionTimeoutMillis)
    throws ComponentPoolRegistrationException;

  public abstract int getInitialPoolSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract int getMinPoolSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setMinPoolSize (String poolId, int minPoolSize)
    throws ComponentPoolRegistrationException;

  public abstract int getMaxPoolSize (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setMaxPoolSize (String poolId, int maxPoolSize)
    throws ComponentPoolRegistrationException;

  public abstract long getAcquireWaitTimeMillis (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setAcquireWaitTimeMillis (String poolId, long acquireWaitTimeMillis)
    throws ComponentPoolRegistrationException;

  public abstract int getMaxLeaseTimeSeconds (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setMaxLeaseTimeSeconds (String poolId, int maxLeaseTimeSeconds)
    throws ComponentPoolRegistrationException;

  public abstract int getMaxIdleTimeSeconds (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setMaxIdleTimeSeconds (String poolId, int maxIdleTimeSeconds)
    throws ComponentPoolRegistrationException;

  public abstract int getUnReturnedElementTimeoutSeconds (String poolId)
    throws ComponentPoolRegistrationException;

  public abstract void setUnReturnedElementTimeoutSeconds (String poolId, int unReturnedElementTimeoutSeconds)
    throws ComponentPoolRegistrationException;
}

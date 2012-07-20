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
package org.smallmind.quorum.pool.connection;

public interface ConnectionPoolSurface {

  public abstract String getPoolName ();

  public abstract void startup ()
    throws ConnectionPoolException;

  public abstract void shutdown ()
    throws ConnectionPoolException;

  public abstract int getPoolSize ();

  public abstract int getFreeSize ();

  public abstract int getProcessingSize ();

  public abstract boolean isTestOnConnect ();

  public abstract void setTestOnConnect (boolean testOnConnect);

  public abstract boolean isTestOnAcquire ();

  public abstract void setTestOnAcquire (boolean testOnAcquire);

  public abstract boolean isReportLeaseTimeNanos ();

  public abstract void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos);

  public abstract boolean isExistentiallyAware ();

  public abstract void setExistentiallyAware (boolean existentiallyAware);

  public abstract long getConnectionTimeoutMillis ();

  public abstract void setConnectionTimeoutMillis (long connectionTimeoutMillis);

  public abstract long getAcquireWaitTimeMillis ();

  public abstract void setAcquireWaitTimeMillis (long acquireWaitTimeMillis);

  public abstract int getInitialPoolSize ();

  public abstract int getMinPoolSize ();

  public abstract void setMinPoolSize (int minPoolSize);

  public abstract int getMaxPoolSize ();

  public abstract void setMaxPoolSize (int maxPoolSize);

  public abstract int getMaxLeaseTimeSeconds ();

  public abstract void setMaxLeaseTimeSeconds (int maxLeaseTimeSeconds);

  public abstract int getMaxIdleTimeSeconds ();

  public abstract void setMaxIdleTimeSeconds (int maxIdleTimeSeconds);

  public abstract int getUnReturnedElementTimeoutSeconds ();

  public abstract void setUnReturnedElementTimeoutSeconds (int unReturnedElementTimeoutSeconds);
}

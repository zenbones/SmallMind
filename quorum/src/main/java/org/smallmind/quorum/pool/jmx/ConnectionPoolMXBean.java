package org.smallmind.quorum.pool.jmx;

import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.PoolMode;

public interface ConnectionPoolMXBean {

   public abstract String getPoolName ();

   public abstract PoolMode getPoolMode ();

   public abstract void setPoolMode (PoolMode poolMode);

   public abstract boolean isTestOnConnect ();

   public abstract void setTestOnConnect (boolean testOnConnect);

   public abstract boolean isTestOnAcquire ();

   public abstract void setTestOnAcquire (boolean testOnAcquire);

   public abstract boolean isReportLeaseTimeNanos ();

   public abstract void setReportLeaseTimeNanos (boolean reportLeaseTimeNanos);

   public abstract long getConnectionTimeoutMillis ();

   public abstract void setConnectionTimeoutMillis (long connectionTimeoutMillis);

   public abstract int getInitialPoolSize ();

   public abstract int getMinPoolSize ();

   public abstract void setMinPoolSize (int minPoolSize);

   public abstract int getMaxPoolSize ();

   public abstract void setMaxPoolSize (int maxPoolSize);

   public abstract int getAcquireRetryAttempts ();

   public abstract void setAcquireRetryAttempts (int acquireRetryAttempts);

   public abstract int getAcquireRetryDelayMillis ();

   public abstract void setAcquireRetryDelayMillis (int acquireRetryDelayMillis);

   public abstract int getMaxLeaseTimeSeconds ();

   public abstract void setMaxLeaseTimeSeconds (int leaseTimeSeconds);

   public abstract int getMaxIdleTimeSeconds ();

   public abstract void setMaxIdleTimeSeconds (int maxIdleTimeSeconds);

   public abstract int getUnreturnedConnectionTimeoutSeconds ();

   public abstract void setUnreturnedConnectionTimeoutSeconds (int unreturnedConnectionTimeoutSeconds);

   public abstract void startup ()
      throws ConnectionPoolException;

   public abstract void shutdown ();

   public abstract int getPoolSize ();

   public abstract int getFreeSize ();

   public abstract int getProcessingSize ();
}

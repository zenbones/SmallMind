package org.smallmind.scheduling.base;

import java.util.Date;

public interface ProxyJob {

   public abstract boolean logOnZeroCount();

   public abstract JobStatus getJobStatus();

   public abstract int getCount();

   public abstract void incCount();

   public abstract Date getStartTime();

   public abstract Date getStopTime();

   public abstract Exception[] getExceptions();

   public abstract void setException(Exception exception);

   public abstract void shutdown()
         throws Exception;

   public abstract void proceed()
         throws Exception;
}
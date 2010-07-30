package org.smallmind.scheduling.quartz;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.smallmind.scheduling.base.JobStatus;
import org.smallmind.scheduling.base.ProxyJob;
import org.smallmind.scribe.pen.LoggerManager;

import java.util.Date;
import java.util.LinkedList;

public abstract class QuartzProxyJob implements ProxyJob, Job {

   private LinkedList<Exception> exceptionList;
   private JobStatus status = JobStatus.SUCCESS;
   private Date startTime;
   private Date stopTime;
   private int count = 0;

   public QuartzProxyJob() {

      exceptionList = new LinkedList<Exception>();
   }

   public JobStatus getJobStatus() {

      return status;
   }

   public Date getStartTime() {

      return startTime;
   }

   public Date getStopTime() {

      return stopTime;
   }

   public synchronized void incCount() {

      count++;
   }

   public synchronized int getCount() {

      return count;
   }

   public synchronized Exception[] getExceptions() {

      if (!exceptionList.isEmpty()) {

         Exception[] exceptions;

         exceptions = new Exception[exceptionList.size()];
         exceptionList.toArray(exceptions);

         return exceptions;
      }

      return null;
   }

   public synchronized void setException(Exception exception) {

      setException(exception, true);
   }

   public synchronized void setException(Exception exception, boolean isFailure) {

      exceptionList.add(exception);
      LoggerManager.getLogger(this.getClass()).error(exception);

      if (isFailure) {
         status = JobStatus.FAILURE;
      }
   }

   public void execute(JobExecutionContext jobExecutionContext)
         throws JobExecutionException {

      startTime = new Date();
      try {
         proceed();
         shutdown();
      }
      catch (Exception exception) {
         setException(exception);
         throw new JobExecutionException(exception);
      }
      finally {
         stopTime = new Date();

         if (status.equals(JobStatus.FAILURE) || (count > 0) || logOnZeroCount()) {
            LoggerManager.getLogger(this.getClass()).info("Job(%s) start(%s) stop(%s) count(%d) state(%s)", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
         }
      }
   }
}
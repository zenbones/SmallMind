package org.smallmind.seda;

import java.util.concurrent.TimeUnit;

public class SedaConfiguration {

   private TimeUnit queuePollTimeUnit;
   private TimeUnit workTrackingTimeUnit;
   private TimeUnit regulatorPulseTimeUnit;
   private long queuePollTimeout;
   private long workTrackingTime;
   private long regulatorPulseTime;
   private int maxQueueCapacity;
   private int minThreadPoolSize;
   private int maxThreadPoolSize;
   private int maxTrackedInvocations;

   public SedaConfiguration () {

      this(Integer.MAX_VALUE, 300, TimeUnit.MILLISECONDS, 1, 0, 3, TimeUnit.SECONDS, 30, 500, TimeUnit.MILLISECONDS);
   }

   public SedaConfiguration (int maxQueueCapacity, long queuePollTimeout, TimeUnit queuePollTimeUnit, int minThreadPoolSize, int maxThreadPoolSize, long workTrackingTime, TimeUnit workTrackingTimeUnit, int maxTrackedInvocations, long regulatorPulseTime, TimeUnit regulatorPulseTimeUnit) {

      this.maxQueueCapacity = maxQueueCapacity;
      this.queuePollTimeout = queuePollTimeout;
      this.queuePollTimeUnit = queuePollTimeUnit;
      this.minThreadPoolSize = minThreadPoolSize;
      this.maxThreadPoolSize = maxThreadPoolSize;
      this.workTrackingTime = workTrackingTime;
      this.workTrackingTimeUnit = workTrackingTimeUnit;
      this.maxTrackedInvocations = maxTrackedInvocations;
      this.regulatorPulseTime = regulatorPulseTime;
      this.regulatorPulseTimeUnit = regulatorPulseTimeUnit;
   }

   public TimeUnit getQueuePollTimeUnit () {

      return queuePollTimeUnit;
   }

   public void setQueuePollTimeUnit (TimeUnit queuePollTimeUnit) {

      this.queuePollTimeUnit = queuePollTimeUnit;
   }

   public TimeUnit getWorkTrackingTimeUnit () {

      return workTrackingTimeUnit;
   }

   public void setWorkTrackingTimeUnit (TimeUnit workTrackingTimeUnit) {

      this.workTrackingTimeUnit = workTrackingTimeUnit;
   }

   public TimeUnit getRegulatorPulseTimeUnit () {

      return regulatorPulseTimeUnit;
   }

   public void setRegulatorPulseTimeUnit (TimeUnit regulatorPulseTimeUnit) {

      this.regulatorPulseTimeUnit = regulatorPulseTimeUnit;
   }

   public long getQueuePollTimeout () {

      return queuePollTimeout;
   }

   public void setQueuePollTimeout (long queuePollTimeout) {

      this.queuePollTimeout = queuePollTimeout;
   }

   public long getWorkTrackingTime () {

      return workTrackingTime;
   }

   public void setWorkTrackingTime (long workTrackingTime) {

      this.workTrackingTime = workTrackingTime;
   }

   public long getRegulatorPulseTime () {

      return regulatorPulseTime;
   }

   public void setRegulatorPulseTime (long regulatorPulseTime) {

      this.regulatorPulseTime = regulatorPulseTime;
   }

   public int getMaxQueueCapacity () {

      return maxQueueCapacity;
   }

   public void setMaxQueueCapacity (int maxQueueCapacity) {

      this.maxQueueCapacity = maxQueueCapacity;
   }

   public int getMinThreadPoolSize () {

      return minThreadPoolSize;
   }

   public void setMinThreadPoolSize (int minThreadPoolSize) {

      this.minThreadPoolSize = minThreadPoolSize;
   }

   public int getMaxThreadPoolSize () {

      return maxThreadPoolSize;
   }

   public void setMaxThreadPoolSize (int maxThreadPoolSize) {

      this.maxThreadPoolSize = maxThreadPoolSize;
   }

   public int getMaxTrackedInvocations () {

      return maxTrackedInvocations;
   }

   public void setMaxTrackedInvocations (int maxTrackedInvocations) {

      this.maxTrackedInvocations = maxTrackedInvocations;
   }
}
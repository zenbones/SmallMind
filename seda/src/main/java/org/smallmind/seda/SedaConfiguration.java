package org.smallmind.seda;

import java.util.concurrent.TimeUnit;

public class SedaConfiguration {

   private TimeUnit queuePollTimeUnit;
   private TimeUnit workTrackingTimeUnit;
   private TimeUnit regulatorPulseTimeUnit;
   private double inactiveDownShiftPercentage;
   private double activeUpShiftPercentage;
   private long queuePollTimeout;
   private long workTrackingTime;
   private long regulatorPulseTime;
   private int maxQueueCapacity;
   private int minThreadPoolSize;
   private int maxThreadPoolSize;
   private int maxTrackedInvocations;

   public SedaConfiguration () {

      setMaxQueueCapacity(Integer.MAX_VALUE);
      setQueuePollTimeout(300);
      setQueuePollTimeUnit(TimeUnit.MILLISECONDS);
      setMinThreadPoolSize(1);
      setMaxThreadPoolSize(0);
      setWorkTrackingTime(3);
      setWorkTrackingTimeUnit(TimeUnit.SECONDS);
      setMaxTrackedInvocations(30);
      setRegulatorPulseTime(500);
      setRegulatorPulseTimeUnit(TimeUnit.MILLISECONDS);
      setInactiveDownShiftPercentage(30);
      setActiveUpShiftPercentage(90);
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

   public double getInactiveDownShiftPercentage () {

      return inactiveDownShiftPercentage;
   }

   public void setInactiveDownShiftPercentage (double inactiveDownShiftPercentage) {

      this.inactiveDownShiftPercentage = inactiveDownShiftPercentage;
   }

   public double getActiveUpShiftPercentage () {

      return activeUpShiftPercentage;
   }

   public void setActiveUpShiftPercentage (double activeUpShiftPercentage) {

      this.activeUpShiftPercentage = activeUpShiftPercentage;
   }
}
/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
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
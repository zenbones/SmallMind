/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
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
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class WorkMonitor {

  private static final double BY_MILLIS = 1000000d;

  private ReentrantReadWriteLock lock;
  private TimeNormalizer idleNormalizer;
  private TimeNormalizer activeNormalizer;
  private DurationMonitor durationMonitor;

  public WorkMonitor (DurationMonitor durationMonitor, long trackingTime, TimeUnit trackingTimeUnit) {

    this.durationMonitor = durationMonitor;

    idleNormalizer = new TimeNormalizer(trackingTimeUnit.toMillis(trackingTime));
    activeNormalizer = new TimeNormalizer(trackingTimeUnit.toMillis(trackingTime));

    lock = new ReentrantReadWriteLock();
  }

  public double getIdlePercentage () {

    lock.readLock().lock();
    try {

      double idleTime;
      double totalTime;

      if ((totalTime = (idleTime = idleNormalizer.getTotal()) + activeNormalizer.getTotal()) == 0) {

        return 0;
      }

      return (idleTime / totalTime) * 100;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  public double getActivePercentage () {

    lock.readLock().lock();
    try {

      double activeTime;
      double totalTime;

      if ((totalTime = idleNormalizer.getTotal() + (activeTime = activeNormalizer.getTotal())) == 0) {

        return 0;
      }

      return (activeTime / totalTime) * 100;
    }
    finally {
      lock.readLock().unlock();
    }
  }

  protected void addIdleTime (StopWatch stopWatch) {

    double end = stopWatch.getStartMillis() + (stopWatch.getDurationNanos() / BY_MILLIS);

    lock.writeLock().lock();
    try {
      idleNormalizer.additional(stopWatch.getStartMillis(), end);
      activeNormalizer.update(end);
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  protected void addActiveTime (StopWatch stopWatch) {

    double end = stopWatch.getStartMillis() + (stopWatch.getDurationNanos() / BY_MILLIS);

    lock.writeLock().lock();
    try {
      activeNormalizer.additional(stopWatch.getStartMillis(), end);
      idleNormalizer.update(end);
      durationMonitor.accumulate(stopWatch.getDurationNanos());
    }
    finally {
      lock.writeLock().unlock();
    }
  }

  private static class TimeNormalizer {

    private boolean initialized = false;
    private double start;
    private double stop;
    private double average;
    private long track;

    public TimeNormalizer (long track) {

      this.track = track;
    }

    public double getTotal () {

      if (!initialized) {

        return 0;
      }

      return (stop - start) * average;
    }

    public void update (double end) {

      if (initialized) {

        double oldest;

        if ((oldest = end - track) > stop) {
          start = 0;
          stop = 0;
          average = 0;
        }
        else {
          oldest = Math.max(start, oldest);
          average = ((stop - oldest) * average) / (end - oldest);
          start = oldest;
          stop = end;
        }
      }
    }

    public void additional (long begin, double end) {

      if (end > begin) {
        if (!initialized) {
          initialized = true;

          start = begin;
          stop = end;
          average = 1;
        }
        else {

          double oldest;

          if ((oldest = end - track) > stop) {
            start = begin;
            average = 1;
          }
          else {
            oldest = Math.max(start, oldest);
            average = (((stop - oldest) * average) + (end - begin)) / (end - oldest);
            start = oldest;
          }

          stop = end;
        }
      }
    }
  }
}

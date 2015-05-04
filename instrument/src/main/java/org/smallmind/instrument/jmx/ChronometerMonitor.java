/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.instrument.jmx;

import java.util.concurrent.TimeUnit;
import javax.management.StandardMBean;
import org.smallmind.instrument.Chronometer;

public class ChronometerMonitor extends StandardMBean implements ChronometerMonitorMXBean {

  private Chronometer chronometer;

  public ChronometerMonitor (Chronometer chronometer) {

    super(ChronometerMonitorMXBean.class, true);

    this.chronometer = chronometer;
  }

  @Override
  public String getSampleType () {

    return chronometer.getSampleType();
  }

  @Override
  public TimeUnit getLatencyTimeUnit () {

    return chronometer.getLatencyTimeUnit();
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return chronometer.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return chronometer.getCount();
  }

  @Override
  public double getOneMinuteAvgRate () {

    return chronometer.getOneMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgRate () {

    return chronometer.getFiveMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgRate () {

    return chronometer.getFifteenMinuteAvgRate();
  }

  @Override
  public double getAverageRate () {

    return chronometer.getAverageRate();
  }

  @Override
  public double getMax () {

    return chronometer.getMax();
  }

  @Override
  public double getMin () {

    return chronometer.getMin();
  }

  @Override
  public double getAverage () {

    return chronometer.getAverage();
  }

  @Override
  public double getStdDev () {

    return chronometer.getStdDev();
  }

  @Override
  public double getSum () {

    return chronometer.getSum();
  }

  @Override
  public double getMedian () {

    return chronometer.getStatistics().getMedian();
  }

  @Override
  public double get75thPercentile () {

    return chronometer.getStatistics().get75thPercentile();
  }

  @Override
  public double get95thPercentile () {

    return chronometer.getStatistics().get95thPercentile();
  }

  @Override
  public double get98thPercentile () {

    return chronometer.getStatistics().get98thPercentile();
  }

  @Override
  public double get99thPercentile () {

    return chronometer.getStatistics().get99thPercentile();
  }

  @Override
  public double get999thPercentile () {

    return chronometer.getStatistics().get999thPercentile();
  }
}

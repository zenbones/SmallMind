/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.instrument.jmx;

import javax.management.StandardMBean;
import org.smallmind.instrument.Histogram;

public class HistogramMonitor extends StandardMBean implements HistogramMonitorMXBean {

  private Histogram histogram;

  public HistogramMonitor (Histogram histogram) {

    super(HistogramMonitorMXBean.class, true);

    this.histogram = histogram;
  }

  @Override
  public String getSampleType () {

    return histogram.getSampleType();
  }

  @Override
  public long getCount () {

    return histogram.getCount();
  }

  @Override
  public double getSum () {

    return histogram.getSum();
  }

  @Override
  public double getMax () {

    return histogram.getMax();
  }

  @Override
  public double getMin () {

    return histogram.getMin();
  }

  @Override
  public double getAverage () {

    return histogram.getAverage();
  }

  @Override
  public double getStdDev () {

    return histogram.getStdDev();
  }

  @Override
  public double getMedian () {

    return histogram.getStatistics().getMedian();
  }

  @Override
  public double get75thPercentile () {

    return histogram.getStatistics().get75thPercentile();
  }

  @Override
  public double get95thPercentile () {

    return histogram.getStatistics().get95thPercentile();
  }

  @Override
  public double get98thPercentile () {

    return histogram.getStatistics().get98thPercentile();
  }

  @Override
  public double get99thPercentile () {

    return histogram.getStatistics().get99thPercentile();
  }

  @Override
  public double get999thPercentile () {

    return histogram.getStatistics().get999thPercentile();
  }
}

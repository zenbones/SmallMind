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
package org.smallmind.instrument;

import java.util.concurrent.TimeUnit;
import org.smallmind.instrument.context.MetricFact;
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;

public class ChronometerImpl extends MetricImpl<Chronometer> implements Chronometer {

  private final Histogram histogram;
  private final Gauge gauge;
  private final TimeUnit durationTimeUnit;

  public ChronometerImpl (Samples samples, TimeUnit durationTimeUnit, long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    this.durationTimeUnit = durationTimeUnit;

    gauge = new GaugeImpl(tickInterval, tickTimeUnit, clock).setName("gauge");
    histogram = new HistogramImpl(samples).setName("histogram");
  }

  @Override
  public Class<Chronometer> getMetricClass () {

    return Chronometer.class;
  }

  @Override
  public void clear () {

    MetricSnapshot metricSnapshot;

    gauge.clear();
    histogram.clear();

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      if (metricSnapshot.willTrace(MetricFact.DURATION)) {
        metricSnapshot.addItem(new MetricItem<>("duration", 0L));
      }
    }
  }

  @Override
  public void update (long duration) {

    if (duration < 0) {
      throw new InstrumentationException("Chronometer durations must be >= 0");
    }

    MetricSnapshot metricSnapshot;

    histogram.update(duration);
    gauge.mark();

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      if (metricSnapshot.willTrace(MetricFact.DURATION)) {
        metricSnapshot.addItem(new MetricItem<>("duration", duration));
      }
    }
  }

  @Override
  public String getSampleType () {

    return histogram.getSampleType();
  }

  @Override
  public Clock getClock () {

    return gauge.getClock();
  }

  @Override
  public TimeUnit getLatencyTimeUnit () {

    return durationTimeUnit;
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return gauge.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return gauge.getCount();
  }

  @Override
  public double getOneMinuteAvgRate () {

    return gauge.getOneMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgRate () {

    return gauge.getFiveMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgRate () {

    return gauge.getFifteenMinuteAvgRate();
  }

  @Override
  public double getAverageRate () {

    return gauge.getAverageRate();
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
  public double getSum () {

    return histogram.getSum();
  }

  @Override
  public Statistics getStatistics () {

    return new Statistics(histogram.getStatistics().getValues());
  }

  @Override
  public void stop () {

    gauge.stop();
  }
}

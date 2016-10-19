/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.instrument.context.MetricFact;
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;

public class SpeedometerImpl extends MetricImpl<Speedometer> implements Speedometer {

  private final Gauge rateGauge;
  private final Gauge quantityGauge;
  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

  public SpeedometerImpl (long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    rateGauge = new GaugeImpl(tickInterval, tickTimeUnit, clock).setName("rate");
    quantityGauge = new GaugeImpl(tickInterval, tickTimeUnit, clock).setName("quantity");
  }

  @Override
  public Class<Speedometer> getMetricClass () {

    return Speedometer.class;
  }

  @Override
  public void clear () {

    MetricSnapshot metricSnapshot;

    rateGauge.clear();
    quantityGauge.clear();
    max.set(Long.MIN_VALUE);
    min.set(Long.MAX_VALUE);

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      if (metricSnapshot.willTrace(MetricFact.MIN)) {
        metricSnapshot.addItem(new MetricItem<>("min", "n/a"));
      }
      if (metricSnapshot.willTrace(MetricFact.MAX)) {
        metricSnapshot.addItem(new MetricItem<>("max", "n/a"));
      }
    }
  }

  @Override
  public void update () {

    update(1);
  }

  @Override
  public void update (long quantity) {

    MetricSnapshot metricSnapshot;
    long currentMin;
    long currentMax;

    rateGauge.mark();
    quantityGauge.mark(quantity);
    currentMin = setMin(quantity);
    currentMax = setMax(quantity);

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      if (metricSnapshot.willTrace(MetricFact.MIN)) {
        metricSnapshot.addItem(new MetricItem<>("min", currentMin));
      }
      if (metricSnapshot.willTrace(MetricFact.MAX)) {
        metricSnapshot.addItem(new MetricItem<>("max", currentMax));
      }
    }
  }

  @Override
  public Clock getClock () {

    return rateGauge.getClock();
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return rateGauge.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return rateGauge.getCount();
  }

  @Override
  public double getOneMinuteAvgRate () {

    return rateGauge.getOneMinuteAvgRate();
  }

  @Override
  public double getOneMinuteAvgVelocity () {

    return quantityGauge.getOneMinuteAvgRate() / rateGauge.getOneMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgRate () {

    return rateGauge.getFiveMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgVelocity () {

    return quantityGauge.getFiveMinuteAvgRate() / rateGauge.getFiveMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgRate () {

    return rateGauge.getFifteenMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgVelocity () {

    return quantityGauge.getFifteenMinuteAvgRate() / rateGauge.getFifteenMinuteAvgRate();
  }

  @Override
  public double getAverageRate () {

    return rateGauge.getAverageRate();
  }

  @Override
  public double getAverageVelocity () {

    return quantityGauge.getAverageRate() / rateGauge.getAverageRate();
  }

  @Override
  public double getMax () {

    return (getCount() > 0) ? max.get() : 0.0;
  }

  private long setMax (long potentialMax) {

    boolean replaced = false;
    long currentMax;

    do {
      currentMax = max.get();
    } while (!(currentMax >= potentialMax || (replaced = max.compareAndSet(currentMax, potentialMax))));

    return replaced ? potentialMax : currentMax;
  }

  @Override
  public double getMin () {

    return (getCount() > 0) ? min.get() : 0.0;
  }

  private long setMin (long potentialMin) {

    boolean replaced = false;
    long currentMin;

    do {
      currentMin = min.get();
    } while (!(currentMin <= potentialMin || (replaced = min.compareAndSet(currentMin, potentialMin))));

    return replaced ? potentialMin : currentMin;
  }

  @Override
  public void stop () {

    rateGauge.stop();
    quantityGauge.stop();
  }
}

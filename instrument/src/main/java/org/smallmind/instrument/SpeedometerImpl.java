/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
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
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;

public class SpeedometerImpl extends Metric implements Speedometer {

  private final MeterImpl rateMeter;
  private final MeterImpl quantityMeter;
  private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong max = new AtomicLong(Long.MIN_VALUE);

  public SpeedometerImpl (long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    rateMeter = new MeterImpl(tickInterval, tickTimeUnit, clock);
    quantityMeter = new MeterImpl(tickInterval, tickTimeUnit, clock);
  }

  @Override
  public void clear () {

    MetricSnapshot snapshot;

    rateMeter.clear();
    quantityMeter.clear();
    min.set(Long.MAX_VALUE);
    max.set(Long.MIN_VALUE);

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("quantity", 0L));
    }
  }

  @Override
  public void update () {

    update(1);
  }

  @Override
  public void update (long quantity) {

    MetricSnapshot snapshot;

    rateMeter.mark();
    quantityMeter.mark(quantity);
    setMax(quantity);
    setMin(quantity);

    if ((snapshot = getMetricSnapshot()) != null) {
      snapshot.addItem(new MetricItem<Long>("quantity", quantity));
    }
  }

  @Override
  public Clock getClock () {

    return rateMeter.getClock();
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return rateMeter.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return rateMeter.getCount();
  }

  @Override
  public double getOneMinuteAvgRate () {

    return rateMeter.getOneMinuteAvgRate();
  }

  @Override
  public double getOneMinuteAvgVelocity () {

    return quantityMeter.getOneMinuteAvgRate() / rateMeter.getOneMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgRate () {

    return rateMeter.getFiveMinuteAvgRate();
  }

  @Override
  public double getFiveMinuteAvgVelocity () {

    return quantityMeter.getFiveMinuteAvgRate() / rateMeter.getFiveMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgRate () {

    return rateMeter.getFifteenMinuteAvgRate();
  }

  @Override
  public double getFifteenMinuteAvgVelocity () {

    return quantityMeter.getFifteenMinuteAvgRate() / rateMeter.getFifteenMinuteAvgRate();
  }

  @Override
  public double getAverageRate () {

    return rateMeter.getAverageRate();
  }

  @Override
  public double getAverageVelocity () {

    return quantityMeter.getAverageRate() / rateMeter.getAverageRate();
  }

  @Override
  public double getMax () {

    return (getCount() > 0) ? max.get() : 0.0;
  }

  private void setMax (long potentialMax) {

    boolean done = false;

    while (!done) {

      long currentMax = max.get();

      done = currentMax >= potentialMax || max.compareAndSet(currentMax, potentialMax);
    }
  }

  @Override
  public double getMin () {

    return (getCount() > 0) ? min.get() : 0.0;
  }

  private void setMin (long potentialMin) {

    boolean done = false;

    while (!done) {

      long currentMin = min.get();

      done = currentMin <= potentialMin || min.compareAndSet(currentMin, potentialMin);
    }
  }

  @Override
  public void stop () {

    rateMeter.stop();
    quantityMeter.stop();
  }
}

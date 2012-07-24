/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Chronometer implements Metric, Metered, Estimating, Timed, Shutterbug, Stoppable {

  private final Histogram histogram;
  private final Meter meter;
  private final Clock clock;
  private final TimeUnit durationTimeUnit;

  Chronometer (Samples samples, TimeUnit durationTimeUnit, long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    this.durationTimeUnit = durationTimeUnit;
    this.clock = clock;

    meter = new Meter(tickInterval, tickTimeUnit, clock);
    histogram = new Histogram(samples);
  }

  public <T> T time (Callable<T> event)
    throws Exception {

    final long startTime = clock.getTick();
    try {
      return event.call();
    }
    finally {
      update(clock.getTick() - startTime);
    }
  }

  public void update (long duration, TimeUnit unit) {

    update(unit.toNanos(duration));
  }

  private void update (long duration) {

    if (duration >= 0) {
      histogram.update(duration);
      meter.mark();
    }
  }

  @Override
  public String getSampleType () {

    return histogram.getSampleType();
  }

  @Override
  public String getLatencyTimeUnit () {

    return durationTimeUnit.name();
  }

  @Override
  public String getRateTimeUnit () {

    return meter.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return meter.getCount();
  }

  @Override
  public double getOneMinuteRate () {

    return meter.getOneMinuteRate();
  }

  @Override
  public double getFiveMinuteRate () {

    return meter.getFiveMinuteRate();
  }

  @Override
  public double getFifteenMinuteRate () {

    return meter.getFifteenMinuteRate();
  }

  @Override
  public double getAverageRate () {

    return meter.getAverageRate();
  }

  @Override
  public double getMax () {

    return convertToDurationTimeUnit(histogram.getMax());
  }

  @Override
  public double getMin () {

    return convertToDurationTimeUnit(histogram.getMin());
  }

  @Override
  public double getAverage () {

    return convertToDurationTimeUnit(histogram.getAverage());
  }

  @Override
  public double getStdDev () {

    return convertToDurationTimeUnit(histogram.getStdDev());
  }

  @Override
  public double getSum () {

    return convertToDurationTimeUnit(histogram.getSum());
  }

  @Override
  public Snapshot getSnapshot () {

    double[] values = histogram.getSnapshot().getValues();
    double[] converted = new double[values.length];

    for (int i = 0; i < values.length; i++) {
      converted[i] = convertToDurationTimeUnit(values[i]);
    }

    return new Snapshot(converted);
  }

  private double convertToDurationTimeUnit (double nanoseconds) {

    return nanoseconds / TimeUnit.NANOSECONDS.convert(1, durationTimeUnit);
  }

  @Override
  public void stop () {

    meter.stop();
  }
}

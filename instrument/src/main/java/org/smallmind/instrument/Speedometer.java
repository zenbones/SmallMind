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

import java.util.concurrent.TimeUnit;

public class Speedometer implements Metric, Metered, Estimating, Shutterbug, Clocked, Stoppable {

  private final Histogram histogram;
  private final Meter usageMeter;
  private final Meter incidentMeter;

  Speedometer (Samples samples, long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    usageMeter = new Meter(tickInterval, tickTimeUnit, clock);
    incidentMeter = new Meter(tickInterval, tickTimeUnit, clock);
    histogram = new Histogram(samples);
  }

  public void update () {

    update(1);
  }

  public void update (long quantity) {

    histogram.update(quantity);
    usageMeter.mark(quantity);
    incidentMeter.mark();
  }

  @Override
  public String getSampleType () {

    return histogram.getSampleType();
  }

  @Override
  public Clock getClock () {

    return usageMeter.getClock();
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return usageMeter.getRateTimeUnit();
  }

  @Override
  public long getCount () {

    return incidentMeter.getCount();
  }

  @Override
  public double getOneMinuteRate () {

    return usageMeter.getOneMinuteRate() / incidentMeter.getOneMinuteRate();
  }

  @Override
  public double getFiveMinuteRate () {

    return usageMeter.getFiveMinuteRate() / incidentMeter.getFiveMinuteRate();
  }

  @Override
  public double getFifteenMinuteRate () {

    return usageMeter.getFifteenMinuteRate() / incidentMeter.getFifteenMinuteRate();
  }

  @Override
  public double getAverageRate () {

    return usageMeter.getAverageRate() / incidentMeter.getAverageRate();
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
  public Snapshot getSnapshot () {

    return new Snapshot(histogram.getSnapshot().getValues());
  }

  @Override
  public void stop () {

    usageMeter.stop();
    incidentMeter.stop();
  }
}

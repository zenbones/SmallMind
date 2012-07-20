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

public class Metrics {

  public static MetricBuilder buildRegister (int initialCount) {

    return new RegisterBuilder(initialCount);
  }

  public static MetricBuilder buildMeter (long tickInterval, TimeUnit tickTimeUnit) {

    return new MeterBuilder(tickInterval, tickTimeUnit, Clocks.NANO);
  }

  public static MetricBuilder buildMeter (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new MeterBuilder(tickInterval, tickTimeUnit, clocks);
  }

  public static HistogramBuilder buildHistogram (Samples samples) {

    return new HistogramBuilder(samples);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit) {

    return new ChronometerBuilder(durationUnit, tickInterval, tickTimeUnit, Clocks.NANO);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new ChronometerBuilder(durationUnit, tickInterval, tickTimeUnit, clocks);
  }

  public static interface MetricBuilder<M extends Metric> {

    public abstract M construct ();
  }

  private static class RegisterBuilder implements MetricBuilder<Register> {

    private int initialCount;

    public RegisterBuilder (int initialCount) {

      this.initialCount = initialCount;
    }

    @Override
    public Register construct () {

      return new Register(initialCount);
    }
  }

  private static class MeterBuilder implements MetricBuilder<Meter> {

    private Clocks clocks;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private MeterBuilder (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Meter construct () {

      return new Meter(tickInterval, tickTimeUnit, clocks.getClock());
    }
  }

  private static class HistogramBuilder implements MetricBuilder<Histogram> {

    private Samples samples;

    private HistogramBuilder (Samples samples) {

      this.samples = samples;
    }

    @Override
    public Histogram construct () {

      return new Histogram(samples);
    }
  }

  private static class ChronometerBuilder implements MetricBuilder<Chronometer> {

    private Clocks clocks;
    private TimeUnit durationUnit;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private ChronometerBuilder (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.durationUnit = durationUnit;
      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Chronometer construct () {

      return new Chronometer(durationUnit, tickInterval, tickTimeUnit, clocks.getClock());
    }
  }
}

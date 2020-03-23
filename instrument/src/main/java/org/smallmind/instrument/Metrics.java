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

public class Metrics {

  public static TallyBuilder buildTally (int initialCount) {

    return new TallyBuilder(initialCount);
  }

  public static GaugeBuilder buildGauge (long tickInterval, TimeUnit tickTimeUnit) {

    return new GaugeBuilder(tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static GaugeBuilder buildGauge (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new GaugeBuilder(tickInterval, tickTimeUnit, clocks);
  }

  public static HistogramBuilder buildHistogram (Samples samples) {

    return new HistogramBuilder(samples);
  }

  public static SpeedometerBuilder buildSpeedometer (long tickInterval, TimeUnit tickTimeUnit) {

    return new SpeedometerBuilder(tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static SpeedometerBuilder buildSpeedometer (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new SpeedometerBuilder(tickInterval, tickTimeUnit, clocks);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit) {

    return new ChronometerBuilder(Samples.BIASED, durationUnit, tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static ChronometerBuilder buildChronometer (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit) {

    return new ChronometerBuilder(samples, durationUnit, tickInterval, tickTimeUnit, Clocks.EPOCH);
  }

  public static ChronometerBuilder buildChronometer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new ChronometerBuilder(Samples.BIASED, durationUnit, tickInterval, tickTimeUnit, clocks);
  }

  public static ChronometerBuilder buildChronometer (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

    return new ChronometerBuilder(samples, durationUnit, tickInterval, tickTimeUnit, clocks);
  }

  public interface MetricBuilder<M extends Metric<M>> {

    Class<M> getMetricClass ();

    MetricType getType ();

    M construct ();
  }

  public static class TallyBuilder implements MetricBuilder<Tally> {

    private int initialCount;

    private TallyBuilder (int initialCount) {

      this.initialCount = initialCount;
    }

    @Override
    public Class<Tally> getMetricClass () {

      return Tally.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.TALLY;
    }

    @Override
    public Tally construct () {

      return new TallyImpl(initialCount);
    }
  }

  public static class GaugeBuilder implements MetricBuilder<Gauge> {

    private Clocks clocks;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private GaugeBuilder (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<Gauge> getMetricClass () {

      return Gauge.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.METER;
    }

    @Override
    public Gauge construct () {

      return new GaugeImpl(tickInterval, tickTimeUnit, clocks.getClock());
    }
  }

  public static class HistogramBuilder implements MetricBuilder<Histogram> {

    private Samples samples;

    private HistogramBuilder (Samples samples) {

      this.samples = samples;
    }

    @Override
    public Class<Histogram> getMetricClass () {

      return Histogram.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.HISTOGRAM;
    }

    @Override
    public Histogram construct () {

      return new HistogramImpl(samples);
    }
  }

  public static class SpeedometerBuilder implements MetricBuilder<Speedometer> {

    private Clocks clocks;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private SpeedometerBuilder (long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<Speedometer> getMetricClass () {

      return Speedometer.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.SPEEDOMETER;
    }

    @Override
    public Speedometer construct () {

      return new SpeedometerImpl(tickInterval, tickTimeUnit, clocks.getClock());
    }
  }

  public static class ChronometerBuilder implements MetricBuilder<Chronometer> {

    private Samples samples;
    private Clocks clocks;
    private TimeUnit durationUnit;
    private TimeUnit tickTimeUnit;
    private long tickInterval;

    private ChronometerBuilder (Samples samples, TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clocks clocks) {

      this.samples = samples;
      this.durationUnit = durationUnit;
      this.tickInterval = tickInterval;
      this.tickTimeUnit = tickTimeUnit;
      this.clocks = clocks;
    }

    @Override
    public Class<Chronometer> getMetricClass () {

      return Chronometer.class;
    }

    @Override
    public MetricType getType () {

      return MetricType.CHRONOMETER;
    }

    @Override
    public Chronometer construct () {

      return new ChronometerImpl(samples, durationUnit, tickInterval, tickTimeUnit, clocks.getClock());
    }
  }
}

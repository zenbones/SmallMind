package org.smallmind.instrument;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class Timer implements Metered, Stoppable, Shutterbug, Summarizing {

  private final Histogram histogram;
  private final Meter meter;
  private final Clock clock;
  private final TimeUnit durationUnit;

  Timer (TimeUnit durationUnit, long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    this.durationUnit = durationUnit;
    this.clock = clock;

    meter = new Meter(tickInterval, tickTimeUnit, clock);
    histogram = new Histogram(Samples.BIASED);
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
  public double getMeanRate () {

    return meter.getMeanRate();
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
  public double getMean () {

    return convertToDurationTimeUnit(histogram.getMean());
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

    return nanoseconds / TimeUnit.NANOSECONDS.convert(1, durationUnit);
  }

  @Override
  public void stop () {

    meter.stop();
  }
}

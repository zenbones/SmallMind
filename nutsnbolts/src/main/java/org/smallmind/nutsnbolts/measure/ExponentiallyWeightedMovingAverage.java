package org.smallmind.nutsnbolts.measure;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ExponentiallyWeightedMovingAverage {

  private final AtomicReference<Double> rate = new AtomicReference<Double>(0.0);
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final AtomicLong unprocessed = new AtomicLong();
  private final double alpha;
  private final double intervalInNanos;

  public static ExponentiallyWeightedMovingAverage lastOneMinute (long tickInterval, TimeUnit tickTimeUnit) {

    return new ExponentiallyWeightedMovingAverage(tickInterval, tickTimeUnit, 1);
  }

  public static ExponentiallyWeightedMovingAverage lastFiveMinutes (long tickInterval, TimeUnit tickTimeUnit) {

    return new ExponentiallyWeightedMovingAverage(tickInterval, tickTimeUnit, 5);
  }

  public static ExponentiallyWeightedMovingAverage lastFifteenMinutes (long tickInterval, TimeUnit tickTimeUnit) {

    return new ExponentiallyWeightedMovingAverage(tickInterval, tickTimeUnit, 15);
  }

  private ExponentiallyWeightedMovingAverage (long tickInterval, TimeUnit tickTimeUnit, int minutes) {

    alpha = 1 - Math.exp(tickInterval / ((double)tickTimeUnit.convert(minutes, TimeUnit.MINUTES)));
    intervalInNanos = tickTimeUnit.toNanos(tickInterval);
  }

  public void update (long n) {

    unprocessed.addAndGet(n);
  }

  public void tick () {

    if (initialized.compareAndSet(false, true)) {
      rate.set(unprocessed.getAndSet(0) / intervalInNanos);
    }
    else {

      double currentRate = rate.get();

      rate.set(currentRate + (alpha * ((unprocessed.getAndSet(0) / intervalInNanos) - currentRate)));
    }
  }

  public double getRate (TimeUnit rateUnit) {

    return rate.get() * rateUnit.toNanos(1);
  }
}

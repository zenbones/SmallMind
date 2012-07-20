package org.smallmind.instrument;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Meter implements Metered, Stoppable {

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2);

  private final AtomicLong count = new AtomicLong(0);
  private final ExponentiallyWeightedMovingAverage m1Rate;
  private final ExponentiallyWeightedMovingAverage m5Rate;
  private final ExponentiallyWeightedMovingAverage m15Rate;
  private final ScheduledFuture<?> future;
  private final Clock clock;
  private final TimeUnit tickTimeUnit;
  private final long startTime;

  public Meter () {

    this(5, TimeUnit.SECONDS, Clocks.NANO.getClock());
  }

  public Meter (long tickInterval, TimeUnit tickTimeUnit) {

    this(tickInterval, tickTimeUnit, Clocks.NANO.getClock());
  }

  Meter (long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    this.tickTimeUnit = tickTimeUnit;
    this.clock = clock;

    startTime = clock.getTick();

    m1Rate = ExponentiallyWeightedMovingAverage.lastOneMinute(tickInterval, tickTimeUnit);
    m5Rate = ExponentiallyWeightedMovingAverage.lastFiveMinutes(tickInterval, tickTimeUnit);
    m15Rate = ExponentiallyWeightedMovingAverage.lastFifteenMinutes(tickInterval, tickTimeUnit);

    this.future = SCHEDULED_EXECUTOR.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run () {

        m1Rate.tick();
        m5Rate.tick();
        m15Rate.tick();
      }
    }, tickInterval, tickInterval, tickTimeUnit);
  }

  public void mark () {

    mark(1);
  }

  public void mark (long n) {

    count.addAndGet(n);
    m1Rate.update(n);
    m5Rate.update(n);
    m15Rate.update(n);
  }

  @Override
  public double getOneMinuteRate () {

    return m1Rate.getRate(tickTimeUnit);
  }

  @Override
  public double getFiveMinuteRate () {

    return m5Rate.getRate(tickTimeUnit);
  }

  @Override
  public double getFifteenMinuteRate () {

    return m15Rate.getRate(tickTimeUnit);
  }

  @Override
  public double getMeanRate () {

    long currentCount = count.get();

    if (currentCount == 0) {

      return 0.0;
    }
    else {

      return (currentCount / (double)(clock.getTick() - startTime)) * (double)tickTimeUnit.toNanos(1);
    }
  }

  @Override
  public void stop () {

    future.cancel(false);
  }
}

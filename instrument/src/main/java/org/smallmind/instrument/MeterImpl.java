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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.smallmind.instrument.context.MetricItem;
import org.smallmind.instrument.context.MetricSnapshot;
import org.smallmind.nutsnbolts.time.TimeUtilities;

public class MeterImpl extends MetricImpl<Meter> implements Meter {

  private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(2, new ThreadFactory() {

    @Override
    public Thread newThread (final Runnable runnable) {

      Thread thread = new Thread(runnable);

      thread.setDaemon(true);

      return thread;
    }
  });
  private final ExponentiallyWeightedMovingAverage m1Average;
  private final ExponentiallyWeightedMovingAverage m5Average;
  private final ExponentiallyWeightedMovingAverage m15Average;
  private final ScheduledFuture<?> future;
  private final AtomicLong startTime;
  private final AtomicLong count = new AtomicLong(0);
  private final Clock clock;
  private final TimeUnit tickTimeUnit;

  public MeterImpl () {

    this(5, TimeUnit.SECONDS, Clocks.EPOCH.getClock());
  }

  public MeterImpl (long tickInterval, TimeUnit tickTimeUnit) {

    this(tickInterval, tickTimeUnit, Clocks.EPOCH.getClock());
  }

  public MeterImpl (long tickInterval, TimeUnit tickTimeUnit, Clock clock) {

    this.tickTimeUnit = tickTimeUnit;
    this.clock = clock;

    startTime = new AtomicLong(clock.getTimeMilliseconds());

    m1Average = ExponentiallyWeightedMovingAverage.lastOneMinute(tickInterval, tickTimeUnit);
    m5Average = ExponentiallyWeightedMovingAverage.lastFiveMinutes(tickInterval, tickTimeUnit);
    m15Average = ExponentiallyWeightedMovingAverage.lastFifteenMinutes(tickInterval, tickTimeUnit);

    future = SCHEDULED_EXECUTOR.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run () {

        m1Average.tick();
        m5Average.tick();
        m15Average.tick();
      }
    }, tickInterval, tickInterval, tickTimeUnit);
  }

  @Override
  public Class<Meter> getMetricClass () {

    return Meter.class;
  }

  @Override
  public void clear () {

    MetricSnapshot metricSnapshot;

    startTime.set(clock.getTimeMilliseconds());
    count.set(0);
    m15Average.clear();
    m15Average.clear();
    m15Average.clear();

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      metricSnapshot.addItem(new MetricItem<Long>("count", 0L));
      metricSnapshot.addItem(new MetricItem<Double>("1 min avg", 0.0));
      metricSnapshot.addItem(new MetricItem<Double>("5 min avg", 0.0));
      metricSnapshot.addItem(new MetricItem<Double>("15 min avg", 0.0));
    }
  }

  @Override
  public void mark () {

    mark(1);
  }

  @Override
  public void mark (long n) {

    MetricSnapshot metricSnapshot;
    long current;

    current = count.addAndGet(n);
    m1Average.update(n);
    m5Average.update(n);
    m15Average.update(n);

    if ((metricSnapshot = getMetricSnapshot()) != null) {
      metricSnapshot.addItem(new MetricItem<Long>("count", current));
      metricSnapshot.addItem(new MetricItem<Double>("1 min avg", m1Average.getMovingAverage(tickTimeUnit)));
      metricSnapshot.addItem(new MetricItem<Double>("5 min avg", m5Average.getMovingAverage(tickTimeUnit)));
      metricSnapshot.addItem(new MetricItem<Double>("15 min avg", m15Average.getMovingAverage(tickTimeUnit)));
    }
  }

  @Override
  public Clock getClock () {

    return clock;
  }

  @Override
  public TimeUnit getRateTimeUnit () {

    return tickTimeUnit;
  }

  @Override
  public long getCount () {

    return count.get();
  }

  @Override
  public double getOneMinuteAvgRate () {

    return m1Average.getMovingAverage(tickTimeUnit);
  }

  @Override
  public double getFiveMinuteAvgRate () {

    return m5Average.getMovingAverage(tickTimeUnit);
  }

  @Override
  public double getFifteenMinuteAvgRate () {

    return m15Average.getMovingAverage(tickTimeUnit);
  }

  @Override
  public double getAverageRate () {

    long currentCount = count.get();

    if (currentCount == 0) {

      return 0.0;
    }
    else {

      return (((double)currentCount) / (clock.getTimeMilliseconds() - startTime.get())) * TimeUtilities.convertToDouble(1, tickTimeUnit, TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void stop () {

    future.cancel(false);
  }
}

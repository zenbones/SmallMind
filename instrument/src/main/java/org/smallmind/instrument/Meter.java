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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class Meter implements Metric, Metered, Stoppable {

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
  public String getRateTimeUnit () {

    return tickTimeUnit.name();
  }

  @Override
  public long getCount () {

    return count.get();
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

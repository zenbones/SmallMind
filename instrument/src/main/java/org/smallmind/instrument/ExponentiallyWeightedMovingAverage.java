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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.time.TimeUtilities;

public class ExponentiallyWeightedMovingAverage {

  private final AtomicReference<Double> average = new AtomicReference<Double>(0.0);
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

    alpha = 1 - Math.exp(-(tickInterval / TimeUtilities.convertToDouble(minutes, TimeUnit.MINUTES, tickTimeUnit)));
    intervalInNanos = tickTimeUnit.toNanos(tickInterval);
  }

  public void clear () {

    initialized.set(false);
  }

  public void update (long n) {

    unprocessed.addAndGet(n);
  }

  public void tick () {

    if (initialized.compareAndSet(false, true)) {
      average.set(unprocessed.getAndSet(0) / intervalInNanos);
    }
    else {

      double currentRate = average.get();

      average.set(currentRate + (alpha * ((unprocessed.getAndSet(0) / intervalInNanos) - currentRate)));
    }
  }

  public double getMovingAverage (TimeUnit rateTimeUnit) {

    return average.get() * rateTimeUnit.toNanos(1);
  }
}

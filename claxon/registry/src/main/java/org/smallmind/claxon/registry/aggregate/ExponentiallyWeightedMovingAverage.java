/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.claxon.registry.aggregate;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.smallmind.claxon.registry.Clock;
import org.smallmind.nutsnbolts.time.StintUtility;

public class ExponentiallyWeightedMovingAverage {

  private final ReentrantLock lock = new ReentrantLock();
  private final ConcurrentLinkedQueue<Long> valueQueue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger size = new AtomicInteger();
  private final Clock clock;
  private final double nanosecondsInWindow;
  private double average = 0;
  private long markTime;

  public ExponentiallyWeightedMovingAverage (Clock clock, long window, TimeUnit windowTimeUnit) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(window, windowTimeUnit, TimeUnit.NANOSECONDS);
    markTime = clock.monotonicTime();
  }

  public void update (long value) {

    if (lock.tryLock()) {
      try {
        sweep(value, 1);
      } finally {
        lock.unlock();
      }
    } else {
      size.incrementAndGet();
      valueQueue.add(value);
    }
  }

  private double sweep (long initialValue, int initialCount) {

    Long unprocessed;
    long now = clock.monotonicTime();
    long accumulated = initialValue;
    int cap = size.get();
    int n = initialCount;

    if (cap > 0) {
      while ((unprocessed = valueQueue.poll()) != null) {
        size.decrementAndGet();
        accumulated += unprocessed;
        if (++n >= cap) {
          break;
        }
      }
    }

    if (markTime == 0) {
      average = ((double)accumulated) / n;
    } else {
      average += (1 - Math.exp(-((now - markTime) / nanosecondsInWindow))) * ((((double)accumulated) / n) - average);
    }

    markTime = now;

    return average;
  }

  public double getMovingAverage () {

    lock.lock();
    try {

      return sweep(0, 0);
    } finally {
      lock.unlock();
    }
  }
}

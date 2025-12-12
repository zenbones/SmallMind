/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Calculates an exponentially weighted moving average over a configured time window.
 */
public class ExponentiallyWeightedMovingAverage {

  private final ReentrantLock lock = new ReentrantLock();
  private final ConcurrentLinkedQueue<Long> valueQueue = new ConcurrentLinkedQueue<>();
  private final AtomicInteger size = new AtomicInteger();
  private final Clock clock;
  private final double nanosecondsInWindow;
  private double average = 0;
  private long markTime;

  /**
   * Creates a moving average using the provided window size.
   *
   * @param clock          clock providing monotonic time
   * @param window         window length
   * @param windowTimeUnit time unit for the window
   */
  public ExponentiallyWeightedMovingAverage (Clock clock, long window, TimeUnit windowTimeUnit) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(window, windowTimeUnit, TimeUnit.NANOSECONDS);
    markTime = clock.monotonicTime();
  }

  /**
   * Adds a new value to the moving average, attempting to process immediately or queueing if contended.
   *
   * @param value value to include
   */
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

  /**
   * Processes any queued updates and recomputes the moving average.
   *
   * @param initialValue initial value to include
   * @param initialCount count contribution for the initial value
   * @return the updated moving average
   */
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

  /**
   * Returns the current moving average, flushing queued updates.
   *
   * @return current moving average
   */
  public double getMovingAverage () {

    lock.lock();
    try {

      return sweep(0, 0);
    } finally {
      lock.unlock();
    }
  }
}

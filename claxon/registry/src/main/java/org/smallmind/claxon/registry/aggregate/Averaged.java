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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe {@link Aggregate} that computes a simple arithmetic mean across all recorded values.
 *
 * <p>Updates that arrive while the accumulator lock is held are staged in a non-blocking
 * {@link ConcurrentLinkedQueue} and folded in lazily by the next thread that acquires the lock
 * (via {@link #sweep()}). This design keeps writers wait-free in the common case while still
 * guaranteeing that no value is lost.</p>
 *
 * <p>{@link #getAverage()} drains the queue, computes the mean, resets the accumulators, and
 * returns the result — making each call return the average over values recorded since the
 * previous call.</p>
 */
public class Averaged implements Aggregate {

  /**
   * Guards {@link #accumulatedValue} and {@link #accumulatedCount} and coordinates {@link #sweep()}.
   */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Staging area for values that could not be immediately accumulated due to lock contention.
   */
  private final ConcurrentLinkedQueue<Long> valueQueue = new ConcurrentLinkedQueue<>();

  /**
   * Number of values currently waiting in {@link #valueQueue}.
   */
  private final AtomicInteger size = new AtomicInteger();

  /**
   * Running sum of all accumulated values since the last {@link #getAverage()} call.
   */
  private long accumulatedValue;

  /**
   * Number of values that have been folded into {@link #accumulatedValue}.
   */
  private int accumulatedCount;

  /**
   * Records a new value into the running sum.
   *
   * <p>If the accumulator lock can be acquired immediately, the value is added directly and
   * any queued values are drained first via {@link #sweep()}. Otherwise the value is enqueued
   * for deferred processing.</p>
   *
   * @param value the measurement to include in the average
   */
  @Override
  public void update (long value) {

    if (lock.tryLock()) {
      try {
        sweep();

        accumulatedValue += value;
        accumulatedCount++;
      } finally {
        lock.unlock();
      }
    } else {
      size.incrementAndGet();
      valueQueue.add(value);
    }
  }

  /**
   * Drains up to the number of values that were queued at the time of the call into the
   * running accumulators.
   *
   * <p>This method must be called with {@link #lock} held. It processes at most {@code cap}
   * entries (the queue depth sampled before iteration begins) to avoid unbounded looping when
   * producers are active.</p>
   */
  public void sweep () {

    int cap = size.get();
    int n = 0;

    if (cap > 0) {

      Long unprocessed;

      while ((unprocessed = valueQueue.poll()) != null) {
        size.decrementAndGet();
        accumulatedValue += unprocessed;
        accumulatedCount++;
        if (++n >= cap) {
          break;
        }
      }
    }
  }

  /**
   * Returns the arithmetic mean of all values recorded since the last invocation and resets
   * the internal accumulators to prepare for the next collection window.
   *
   * <p>This method acquires the accumulator lock, flushes any queued values via {@link #sweep()},
   * computes the mean, zeroes the accumulators, and releases the lock.</p>
   *
   * @return the arithmetic mean of all recorded values; {@link Double#NaN} if no values were
   * recorded (division by zero)
   */
  public double getAverage () {

    lock.lock();
    try {

      double average;

      sweep();

      average = accumulatedValue / ((double)accumulatedCount);

      accumulatedValue = 0;
      accumulatedCount = 0;

      return average;
    } finally {
      lock.unlock();
    }
  }
}

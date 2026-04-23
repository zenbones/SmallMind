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
 * Computes an exponentially weighted moving average (EWMA) over a configurable time window.
 *
 * <p>The EWMA is updated using the continuous-decay formula:</p>
 * <pre>
 *   average += (1 - exp(-(elapsed / windowNanos))) * (instantaneousMean - average)
 * </pre>
 * <p>where {@code elapsed} is the nanoseconds since the last update and {@code instantaneousMean}
 * is the mean of all values batched since the last update. This allows the weight given to new
 * observations to reflect how much real time has passed relative to the configured window.</p>
 *
 * <p>Updates that arrive while the internal lock is held are staged in a
 * {@link ConcurrentLinkedQueue} and folded in lazily, keeping writers non-blocking in the
 * common case.</p>
 */
public class ExponentiallyWeightedMovingAverage {

  /**
   * Guards the mutable average state and coordinates queue draining.
   */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Staging area for values that could not be immediately processed due to lock contention.
   */
  private final ConcurrentLinkedQueue<Long> valueQueue = new ConcurrentLinkedQueue<>();

  /**
   * Number of values currently waiting in {@link #valueQueue}.
   */
  private final AtomicInteger size = new AtomicInteger();

  /**
   * Source of monotonic timestamps used to compute elapsed time between updates.
   */
  private final Clock clock;

  /**
   * The configured window duration expressed in nanoseconds, used as the decay time constant.
   */
  private final double nanosecondsInWindow;

  /**
   * Current exponentially weighted moving average; {@code 0} until the first update.
   */
  private double average = 0;

  /**
   * Monotonic timestamp (nanoseconds) of the most recent {@link #sweep} call; {@code 0} signals
   * that the average has not yet been initialised.
   */
  private long markTime;

  /**
   * Constructs an EWMA with the given decay window.
   *
   * @param clock          source of monotonic time used to compute elapsed intervals
   * @param window         length of the decay window
   * @param windowTimeUnit time unit in which {@code window} is expressed
   */
  public ExponentiallyWeightedMovingAverage (Clock clock, long window, TimeUnit windowTimeUnit) {

    this.clock = clock;

    nanosecondsInWindow = StintUtility.convertToDouble(window, windowTimeUnit, TimeUnit.NANOSECONDS);
    markTime = clock.monotonicTime();
  }

  /**
   * Records a new value, attempting immediate integration or queueing when contended.
   *
   * <p>If the lock is immediately available the value is processed inside {@link #sweep}
   * along with any already-queued values. Otherwise the value is enqueued for deferred
   * processing on the next successful lock acquisition.</p>
   *
   * @param value the measurement to include in the moving average
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
   * Drains the pending queue, combines all values with {@code initialValue}, and applies one
   * EWMA update step.
   *
   * <p>Must be called with {@link #lock} held. On the very first call ({@link #markTime} is the
   * construction time and is treated as a valid timestamp), the running average is seeded with
   * the plain mean of the batch. Subsequent calls apply the continuous-decay formula.</p>
   *
   * @param initialValue value contributed by the caller before queue processing begins;
   *                     pass {@code 0} when invoking from {@link #getMovingAverage()} with no new data
   * @param initialCount count contribution for {@code initialValue};
   *                     pass {@code 0} when invoking from {@link #getMovingAverage()} with no new data
   * @return the updated moving average after incorporating all batched values
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
   * Returns the current moving average after flushing any queued values.
   *
   * <p>This method acquires the lock, invokes {@link #sweep} with a zero-contribution seed
   * (so only queued values and elapsed time affect the result), and returns the updated average.</p>
   *
   * @return the exponentially weighted moving average as of this moment
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

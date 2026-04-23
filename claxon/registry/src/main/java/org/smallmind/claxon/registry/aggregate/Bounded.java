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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAccumulator;

/**
 * Thread-safe {@link Aggregate} that independently tracks the running minimum and maximum of
 * all recorded values using a double-buffered accumulator strategy.
 *
 * <p>Each statistic (max and min) is maintained by a pair of {@link LongAccumulator} instances
 * (called "flip" and "flop") together with an {@link AtomicBoolean} flag that indicates which
 * accumulator is currently active for writers. When a reader calls {@link #getMaximum()} or
 * {@link #getMinimum()}, the flag is toggled so that subsequent writes go to the idle
 * accumulator, and the previously active accumulator is drained with
 * {@link LongAccumulator#getThenReset()}. This avoids locking writers during reads at the cost
 * of a brief window in which writes may land in either accumulator.</p>
 */
public class Bounded implements Aggregate {

  /**
   * Active-window maximum accumulator used when {@link #maxFlag} is {@code false}.
   */
  private final LongAccumulator flipMaxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);

  /**
   * Active-window minimum accumulator used when {@link #minFlag} is {@code false}.
   */
  private final LongAccumulator flipMinAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);

  /**
   * Idle-window maximum accumulator used when {@link #maxFlag} is {@code true}.
   */
  private final LongAccumulator flopMaxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);

  /**
   * Idle-window minimum accumulator used when {@link #minFlag} is {@code true}.
   */
  private final LongAccumulator flopMinAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);

  /**
   * Selects which maximum accumulator receives writes; {@code false} means {@link #flipMaxAccumulator},
   * {@code true} means {@link #flopMaxAccumulator}.
   */
  private final AtomicBoolean maxFlag = new AtomicBoolean();

  /**
   * Selects which minimum accumulator receives writes; {@code false} means {@link #flipMinAccumulator},
   * {@code true} means {@link #flopMinAccumulator}.
   */
  private final AtomicBoolean minFlag = new AtomicBoolean();

  /**
   * Records {@code value} into the currently active maximum and minimum accumulators.
   *
   * @param value the measurement to compare against the running extremes
   */
  @Override
  public void update (long value) {

    if (!maxFlag.get()) {
      flipMaxAccumulator.accumulate(value);
    } else {
      flopMaxAccumulator.accumulate(value);
    }

    if (!minFlag.get()) {
      flipMinAccumulator.accumulate(value);
    } else {
      flopMinAccumulator.accumulate(value);
    }
  }

  /**
   * Returns the maximum value recorded since the last call and atomically resets that window's
   * accumulator so the next window starts fresh.
   *
   * <p>The flag is toggled before reading so that concurrent writers switch to the idle
   * accumulator, minimising the chance of a value being missed or double-counted.</p>
   *
   * @return the maximum value seen since the previous invocation; {@link Long#MIN_VALUE} if no
   * values were recorded in the window
   */
  public synchronized long getMaximum () {

    boolean currentFlag = maxFlag.get();

    maxFlag.set(!currentFlag);

    return (!currentFlag) ? flipMaxAccumulator.getThenReset() : flopMaxAccumulator.getThenReset();
  }

  /**
   * Returns the minimum value recorded since the last call and atomically resets that window's
   * accumulator so the next window starts fresh.
   *
   * <p>The flag is toggled before reading so that concurrent writers switch to the idle
   * accumulator, minimising the chance of a value being missed or double-counted.</p>
   *
   * @return the minimum value seen since the previous invocation; {@link Long#MAX_VALUE} if no
   * values were recorded in the window
   */
  public long getMinimum () {

    boolean currentFlag = minFlag.get();

    minFlag.set(!currentFlag);

    return (!currentFlag) ? flipMinAccumulator.getThenReset() : flopMinAccumulator.getThenReset();
  }
}

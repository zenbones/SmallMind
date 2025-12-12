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
 * Tracks minimum and maximum values using double-buffered accumulators to avoid contention during reads.
 */
public class Bounded implements Aggregate {

  private final LongAccumulator flipMaxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
  private final LongAccumulator flipMinAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);
  private final LongAccumulator flopMaxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
  private final LongAccumulator flopMinAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);
  private final AtomicBoolean maxFlag = new AtomicBoolean();
  private final AtomicBoolean minFlag = new AtomicBoolean();

  /**
   * Incorporates a new value into both min and max tracking.
   *
   * @param value value to add
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
   * Returns the current maximum and resets the accumulator for the next window.
   *
   * @return maximum value seen since the last call
   */
  public synchronized long getMaximum () {

    boolean currentFlag = maxFlag.get();

    maxFlag.set(!currentFlag);

    return (!currentFlag) ? flipMaxAccumulator.getThenReset() : flopMaxAccumulator.getThenReset();
  }

  /**
   * Returns the current minimum and resets the accumulator for the next window.
   *
   * @return minimum value seen since the last call
   */
  public long getMinimum () {

    boolean currentFlag = minFlag.get();

    minFlag.set(!currentFlag);

    return (!currentFlag) ? flipMinAccumulator.getThenReset() : flopMinAccumulator.getThenReset();
  }
}

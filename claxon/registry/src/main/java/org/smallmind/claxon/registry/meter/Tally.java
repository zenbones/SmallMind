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
package org.smallmind.claxon.registry.meter;

import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.claxon.registry.aggregate.Tallied;

/**
 * A {@link Meter} that maintains a monotonically accumulating counter by summing
 * every delta supplied via {@link #update(long)}.
 *
 * <p>The underlying {@link Tallied} aggregate accumulates values without resetting
 * between {@link #record()} calls, making {@code Tally} appropriate for tracking
 * running totals such as byte counts, error counts, or any other quantity where the
 * absolute cumulative sum is meaningful to a monitoring back end.</p>
 *
 * <p>On each call to {@link #record()}, a single {@link Quantity} named {@code "count"}
 * with type {@link QuantityType#COUNT} is returned.</p>
 */
public class Tally implements Meter {

  /**
   * Aggregate that accumulates the running total of all submitted deltas.
   */
  private final Tallied tallied;

  /**
   * Creates a new {@code Tally} meter with a freshly initialised {@link Tallied} aggregate
   * starting at zero.
   */
  public Tally () {

    tallied = new Tallied();
  }

  /**
   * Adds {@code value} to the running total maintained by the {@link Tallied} aggregate.
   *
   * @param value the delta to add to the running count; may be negative to decrement the total
   */
  @Override
  public void update (long value) {

    tallied.update(value);
  }

  /**
   * Returns the current accumulated count as a single {@link Quantity}.
   *
   * <p>The returned array always contains exactly one element: {@code "count"} with
   * type {@link QuantityType#COUNT} reflecting the cumulative sum of all values
   * passed to {@link #update(long)} since this meter was created.</p>
   *
   * @return an array containing exactly one {@link Quantity} named {@code "count"}
   */
  @Override
  public Quantity[] record () {

    return new Quantity[] {new Quantity("count", tallied.getCount(), QuantityType.COUNT)};
  }
}

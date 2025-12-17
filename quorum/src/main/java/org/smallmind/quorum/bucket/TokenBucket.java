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
package org.smallmind.quorum.bucket;

import java.util.HashMap;
import org.smallmind.nutsnbolts.time.Stint;

/**
 * Implements a hierarchical token bucket. A bucket tracks its own capacity and optional child buckets so
 * callers can rate limit both a global flow and arbitrary partitions selected from the input stream.
 *
 * @param <T> the type of object being rate limited
 */
public class TokenBucket<T> {

  private final BucketQuantifier<T> quantifier;
  private final BucketSelector<T> selector;
  private final double limit;
  private final double refillPerNanosecond;
  private HashMap<BucketKey<T>, TokenBucket<T>> children;
  private double capacity;
  private long timestamp;

  /**
   * Creates a bucket with a capacity limit and a refill rate. A selector and factory allow optional child buckets
   * keyed by data derived from the input.
   *
   * @param quantifier     strategy that converts inputs into token quantities
   * @param selector       strategy that selects the child bucket key for an input
   * @param limit          maximum capacity of this bucket
   * @param refillQuantity number of tokens refilled per {@code refillRate}
   * @param refillRate     time unit used to calculate the refill rate
   */
  public TokenBucket (BucketQuantifier<T> quantifier, BucketSelector<T> selector, double limit, double refillQuantity, Stint refillRate) {

    this.quantifier = quantifier;
    this.selector = selector;
    this.limit = limit;

    refillPerNanosecond = refillQuantity / (double)refillRate.getTimeUnit().toNanos(refillRate.getTime());
    System.out.println(refillPerNanosecond);

    capacity = limit;
    timestamp = System.nanoTime();
  }

  /**
   * Adds a lazily-created child bucket keyed by the supplied identifier using the provided factory.
   *
   * @param key     key for the child bucket
   * @param factory supplier used to construct the child bucket if it is missing
   */
  public synchronized void add (BucketKey<T> key, BucketFactory<T> factory) {

    if (children == null) {
      children = new HashMap<>();
    }

    if (!children.containsKey(key)) {
      children.put(key, factory.create());
    }
  }

  /**
   * Determines whether the supplied input is allowed at the current time according to the token budget.
   *
   * @param input input to evaluate
   * @return {@code true} if sufficient capacity exists (including any child bucket constraints), otherwise {@code false}
   */
  public synchronized boolean allowed (T input) {

    return allowed(System.nanoTime(), input);
  }

  // synchronized in the case where a child might be shared between two bucket hierarchies

  /**
   * Shared implementation that advances refill state to the supplied timestamp and checks the requested quantity.
   *
   * @param current current time in nanoseconds
   * @param input   input to evaluate
   * @return {@code true} if the input can be consumed, {@code false} otherwise
   */
  private synchronized boolean allowed (long current, T input) {

    double quantity;

    if (current > timestamp) {
      if ((capacity += (current - timestamp) * refillPerNanosecond) > limit) {
        capacity = limit;
      }

      timestamp = current;
    }

    if ((quantity = quantifier.quantity(input)) <= capacity) {

      TokenBucket<T> child;

      if ((children == null) || children.isEmpty() || ((child = children.get(selector.selection(input))) == null) || child.allowed(current, input)) {
        capacity -= quantity;

        return true;
      }
    }

    return false;
  }
}

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
 * Thread-safe token-bucket rate limiter with optional per-key child buckets.
 * <p>
 * Tokens accumulate at a configured rate up to a maximum capacity. Each call to
 * {@link #allowed(Object)} checks the current token balance and, when sufficient
 * tokens are available, deducts the cost of the input and returns {@code true}.
 * An optional hierarchy of child buckets allows a single instance to enforce both
 * an aggregate limit and independent per-partition limits simultaneously.
 *
 * @param <T> the type of value being rate-limited
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
   * Constructs a token bucket with the given capacity limit and refill rate.
   * <p>
   * The bucket starts fully charged. A {@code selector} is required so that
   * child buckets registered via {@link #add(BucketKey, BucketFactory)} can be
   * looked up when evaluating an input; it may safely return {@code null} when
   * no child hierarchy is needed.
   *
   * @param quantifier     converts an input value to the number of tokens it costs
   * @param selector       maps an input value to the child-bucket key that governs it
   * @param limit          maximum token capacity; the bucket is initialized to this value
   * @param refillQuantity number of tokens added per {@code refillRate} period
   * @param refillRate     duration of one refill period expressed as a {@link Stint}
   */
  public TokenBucket (BucketQuantifier<T> quantifier, BucketSelector<T> selector, double limit, double refillQuantity, Stint refillRate) {

    this.quantifier = quantifier;
    this.selector = selector;
    this.limit = limit;

    refillPerNanosecond = refillQuantity / (double)refillRate.getTimeUnit().toNanos(refillRate.getTime());

    capacity = limit;
    timestamp = System.nanoTime();
  }

  /**
   * Registers a child bucket for the given key, creating it via {@code factory} if absent.
   * <p>
   * An input passes this bucket only when it also passes the child bucket whose key
   * matches the value returned by the selector. If the key is already registered the
   * existing child is kept and {@code factory} is not called.
   *
   * @param key     identifier used to look up the child bucket during {@link #allowed(Object)}
   * @param factory creates the child bucket when no entry yet exists for {@code key}
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
   * Tests whether {@code input} is permitted under the current token budget.
   * <p>
   * The bucket is first refilled based on elapsed time, then the token cost of
   * {@code input} is compared against the available balance. If sufficient tokens
   * exist, and every applicable child bucket also permits the input, the tokens are
   * deducted and {@code true} is returned. Otherwise the bucket is unchanged and
   * {@code false} is returned.
   *
   * @param input the value to evaluate
   * @return {@code true} if the input is within rate limits and tokens have been deducted;
   * {@code false} if the input would exceed the budget
   */
  public synchronized boolean allowed (T input) {

    return allowed(System.nanoTime(), input);
  }

  // synchronized in the case where a child might be shared between two bucket hierarchies

  /**
   * Internal implementation shared by the public entry point and recursive child checks.
   * <p>
   * Refills the bucket to {@code current} nanoseconds, then evaluates the token cost.
   * When a matching child bucket exists the check is delegated recursively using the same
   * {@code current} timestamp so the entire hierarchy shares one consistent clock reading.
   *
   * @param current the current time in nanoseconds, used for refill calculation
   * @param input   the value being evaluated
   * @return {@code true} if this bucket (and any relevant child) permits the input
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

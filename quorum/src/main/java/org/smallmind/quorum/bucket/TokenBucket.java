/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class TokenBucket<T> {

  private final BucketQuantifier<T> quantifier;
  private final BucketSelector<T> selector;
  private final double limit;
  private final double refillPerNanosecond;
  private HashMap<BucketKey<T>, TokenBucket<T>> children;
  private double capacity;
  private long timestamp;

  public TokenBucket (BucketQuantifier<T> quantifier, BucketSelector<T> selector, double limit, double refillQuantity, Stint refillRate) {

    this.quantifier = quantifier;
    this.selector = selector;
    this.limit = limit;

    refillPerNanosecond = refillQuantity / (double)refillRate.getTimeUnit().toNanos(refillRate.getTime());
    System.out.println(refillPerNanosecond);

    capacity = limit;
    timestamp = System.nanoTime();
  }

  public synchronized void add (BucketKey<T> key, BucketFactory<T> factory) {

    if (children == null) {
      children = new HashMap<>();
    }

    if (!children.containsKey(key)) {
      children.put(key, factory.create());
    }
  }

  public synchronized boolean allowed (T input) {

    return allowed(System.nanoTime(), input);
  }

  // synchronized in the case where a child might be shared between two bucket hierarchies
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

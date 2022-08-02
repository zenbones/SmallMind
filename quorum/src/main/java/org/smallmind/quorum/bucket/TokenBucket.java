package org.smallmind.quorum.bucket;

import java.util.HashMap;
import org.smallmind.nutsnbolts.time.Stint;

public class TokenBucket<T> {

  private final BucketQuantifier<T> quantifier;
  private final BucketSelector<T> selector;
  private final double limit;
  private final double refillPerNanosecond;
  private HashMap<String, TokenBucket<T>> children;
  private double capacity;
  private long timestamp;

  public TokenBucket (BucketQuantifier<T> quantifier, BucketSelector<T> selector, double limit, double refillQuantity, Stint refillRate) {

    this.quantifier = quantifier;
    this.selector = selector;
    this.limit = limit;

    refillPerNanosecond = refillQuantity / (double)refillRate.getTimeUnit().toNanos(refillRate.getTime());

    capacity = limit;
    timestamp = System.nanoTime();
  }

  public synchronized void add (String key, BucketFactory<T> factory) {

    if (children == null) {
      children = new HashMap<>();
    }

    if (!children.containsKey(key)) {
      children.put(key, factory.create());
    }
  }

  public synchronized boolean allowed (T input) {

    double quantity;
    long current = System.nanoTime();

    if (current > timestamp) {
      if ((capacity += (current - timestamp) * refillPerNanosecond) > limit) {
        capacity = limit;

        timestamp = current;
      }
    }

    if ((quantity = quantifier.quantity(input)) <= capacity) {

      TokenBucket<T> child;

      if ((children == null) || children.isEmpty() || ((child = children.get(selector.selection(input))) == null) || child.allowed(input)) {
        capacity -= quantity;

        return true;
      }
    }

    return false;
  }
}

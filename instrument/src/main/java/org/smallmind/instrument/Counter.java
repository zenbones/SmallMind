package org.smallmind.instrument;

import java.util.concurrent.atomic.AtomicLong;

public class Counter implements Measure {

  private final AtomicLong count;

  public Counter () {

    this(0);
  }

  public Counter (int initialCount) {

    this.count = new AtomicLong(initialCount);
  }

  public void inc () {

    count.incrementAndGet();
  }

  public void inc (long n) {

    count.addAndGet(n);
  }

  public void dec () {

    count.decrementAndGet();
  }

  public void dec (long n) {

    count.addAndGet(0 - n);
  }

  public long getCount () {

    return count.get();
  }

  public void clear () {

    count.set(0);
  }
}

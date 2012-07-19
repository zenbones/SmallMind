package org.smallmind.nutsnbolts.measure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import org.smallmind.nutsnbolts.util.ThreadLocalRandom;

/**
 * Uses Vitter's Algorithm R to produce a statistically representative sample.
 */
public class UniformSample implements Sample {

  private static final int BITS_PER_LONG = 63;

  private final AtomicLong count = new AtomicLong();
  private final AtomicLongArray values;

  public UniformSample (int reservoirSize) {

    this.values = new AtomicLongArray(reservoirSize);
    clear();
  }

  @Override
  public void clear () {

    for (int i = 0; i < values.length(); i++) {
      values.set(i, 0);
    }

    count.set(0);
  }

  @Override
  public int size () {

    long currentCount = count.get();

    return (currentCount > values.length()) ? values.length() : (int)currentCount;
  }

  @Override
  public void update (long value) {

    long updatedCount = count.incrementAndGet();

    if (updatedCount <= values.length()) {
      values.set((int)updatedCount - 1, value);
    }
    else {

      long randomLong = nextLong(updatedCount);

      if (randomLong < values.length()) {
        values.set((int)randomLong, value);
      }
    }
  }

  private long nextLong (long n) {

    long bits;
    long val;

    do {
      bits = ThreadLocalRandom.current().nextLong() & (~(1L << BITS_PER_LONG));
    } while (bits - (val = bits % n) + (n - 1) < 0L);

    return val;
  }

  @Override
  public Snapshot getSnapshot () {

    int currentSize = size();
    List<Long> copy = new ArrayList<Long>(currentSize);

    for (int i = 0; i < currentSize; i++) {
      copy.add(values.get(i));
    }

    return new Snapshot(copy);
  }
}

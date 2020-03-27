package org.smallmind.instrument.micrometer;

import java.util.concurrent.atomic.LongAccumulator;

public class Bounded implements Updatable {

  private final LongAccumulator maxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
  private final LongAccumulator minAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);

  public void update (long value) {

    maxAccumulator.accumulate(value);
    minAccumulator.accumulate(value);
  }

  public long getMaximum () {

    return maxAccumulator.get();
  }

  public long getMinimum () {

    return minAccumulator.get();
  }
}

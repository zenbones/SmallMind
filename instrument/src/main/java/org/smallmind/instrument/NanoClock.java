package org.smallmind.instrument;

public class NanoClock extends Clock {

  @Override
  public long getTick () {

    return System.nanoTime();
  }
}

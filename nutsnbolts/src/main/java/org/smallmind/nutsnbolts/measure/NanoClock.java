package org.smallmind.nutsnbolts.measure;

public class NanoClock extends Clock {

  @Override
  public long getTick () {

    return System.nanoTime();
  }
}

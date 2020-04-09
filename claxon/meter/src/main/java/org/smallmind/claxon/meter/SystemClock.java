package org.smallmind.claxon.meter;

public class SystemClock implements Clock {

  @Override
  public long wallTime () {

    return System.currentTimeMillis();
  }

  @Override
  public long monotonicTime () {

    return System.nanoTime();
  }
}

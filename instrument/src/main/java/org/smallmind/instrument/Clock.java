package org.smallmind.instrument;

public abstract class Clock {

  public abstract long getTick ();

  public long getTime () {

    return System.currentTimeMillis();
  }
}

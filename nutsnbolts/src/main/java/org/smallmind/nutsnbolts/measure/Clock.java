package org.smallmind.nutsnbolts.measure;

public abstract class Clock {

  public abstract long getTick ();

  public long getTime () {

    return System.currentTimeMillis();
  }
}

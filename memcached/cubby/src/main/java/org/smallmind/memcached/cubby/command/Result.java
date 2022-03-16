package org.smallmind.memcached.cubby.command;

public class Result<T> {

  private final T value;
  private final boolean hit;
  private final long cas;

  public Result (T value, boolean hit, long cas) {

    this.value = value;
    this.hit = hit;
    this.cas = cas;
  }

  public T getValue () {

    return value;
  }

  public boolean isHit () {

    return hit;
  }

  public long getCas () {

    return cas;
  }
}

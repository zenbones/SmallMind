package org.smallmind.memcached.cubby;

import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;

public class CubbyMemcachedClient {

  private final Stint timeoutStint;

  public CubbyMemcachedClient () {

    this(new Stint(3, TimeUnit.SECONDS));
  }

  public CubbyMemcachedClient (Stint timeoutStint) {

    this.timeoutStint = timeoutStint;
  }

  public Stint getTimeoutStint () {

    return timeoutStint;
  }
}

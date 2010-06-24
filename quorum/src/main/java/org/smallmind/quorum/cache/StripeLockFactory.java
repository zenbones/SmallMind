package org.smallmind.quorum.cache;

import java.util.concurrent.locks.ReentrantLock;

public class StripeLockFactory {

  public static ReentrantLock[] createStripeLockArray (int concurrencyLevel) {

    ReentrantLock[] stripeLocks;

    if ((concurrencyLevel <= 0) || (concurrencyLevel % 2 != 0)) {
      throw new CacheException("Concurrency level(%d) must be > 0 and an even power of 2", concurrencyLevel);
    }

    stripeLocks = new ReentrantLock[concurrencyLevel];

    for (int count = 0; count < stripeLocks.length; count++) {
      stripeLocks[count] = new ReentrantLock();
    }

    return stripeLocks;
  }
}

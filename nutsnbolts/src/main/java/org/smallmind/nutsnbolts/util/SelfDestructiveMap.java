/*
 * Copyright (c) 2007 through 2024 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 *
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * ...or...
 *
 * 2) The terms of the Apache License, Version 2.0.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.nutsnbolts.util;

import java.util.NavigableSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.nutsnbolts.time.Stint;

public class SelfDestructiveMap<K extends Comparable<K>, S extends SelfDestructive> {

  private final ConcurrentHashMap<K, S> internalMap = new ConcurrentHashMap<K, S>();
  private final ConcurrentSkipListSet<SelfDestructiveKey<K>> ignitionKeySet = new ConcurrentSkipListSet<>();
  private final Stint defaultTimeoutStint;
  private final Stint pulseTimeStint;
  private final IgnitionWorker ignitionWorker;

  public SelfDestructiveMap (Stint defaultTimeoutStint) {

    this(defaultTimeoutStint, new Stint(1, TimeUnit.SECONDS));
  }

  public SelfDestructiveMap (Stint defaultTimeoutStint, Stint pulseTimeStint) {

    Thread ignitionThread;

    this.defaultTimeoutStint = defaultTimeoutStint;
    this.pulseTimeStint = pulseTimeStint;

    ignitionThread = new Thread(ignitionWorker = new IgnitionWorker());
    ignitionThread.setDaemon(true);
    ignitionThread.start();
  }

  public S get (K key) {

    return internalMap.get(key);
  }

  public S putIfAbsent (K key, S value) {

    return putIfAbsent(key, value, defaultTimeoutStint);
  }

  public S putIfAbsent (K key, S value, Stint timeoutStint) {

    S previousValue;

    if ((previousValue = internalMap.putIfAbsent(key, value)) == null) {
      ignitionKeySet.add(new SelfDestructiveKey<>(key, (timeoutStint != null) ? timeoutStint : defaultTimeoutStint));
    }

    return previousValue;
  }

  public void shutdown ()
    throws InterruptedException {

    ignitionWorker.shutdown();
  }

  private class IgnitionWorker implements Runnable {

    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private Thread runnableThread;

    public void shutdown ()
      throws InterruptedException {

      terminationLatch.countDown();

      if (runnableThread != null) {
        runnableThread.interrupt();
      }

      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        runnableThread = Thread.currentThread();

        while (!terminationLatch.await(pulseTimeStint.getTime(), pulseTimeStint.getTimeUnit())) {

          NavigableSet<SelfDestructiveKey<K>> ignitedKeySet;

          if (!(ignitedKeySet = ignitionKeySet.headSet(new SelfDestructiveKey<>(Stint.none()))).isEmpty()) {

            SelfDestructiveKey<K> ignitedKey;

            while ((ignitedKey = ignitedKeySet.pollFirst()) != null) {

              SelfDestructive selfDestructive;

              if ((selfDestructive = internalMap.remove(ignitedKey.getMapKey())) != null) {
                selfDestructive.destroy(ignitedKey.getTimeoutStint());
              }
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        terminationLatch.countDown();
      }

      exitLatch.countDown();
    }
  }
}

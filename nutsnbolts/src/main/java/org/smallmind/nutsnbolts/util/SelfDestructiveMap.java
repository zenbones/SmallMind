/*
 * Copyright (c) 2007 through 2026 David Berkman
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

/**
 * Thread-safe map whose entries are automatically removed after a configurable timeout, invoking {@link SelfDestructive#destroy(Stint)} on each expired value.
 *
 * @param <K> key type, must be {@link Comparable}
 * @param <S> value type, must implement {@link SelfDestructive}
 */
public class SelfDestructiveMap<K extends Comparable<K>, S extends SelfDestructive> {

  private final ConcurrentHashMap<K, S> internalMap = new ConcurrentHashMap<K, S>();
  private final ConcurrentSkipListSet<SelfDestructiveKey<K>> ignitionKeySet = new ConcurrentSkipListSet<>();
  private final Stint defaultTimeoutStint;
  private final Stint pulseTimeStint;
  private final IgnitionWorker ignitionWorker;

  /**
   * Creates a map using the specified default timeout and a one-second background cleanup pulse.
   *
   * @param defaultTimeoutStint timeout applied to entries that do not specify their own
   */
  public SelfDestructiveMap (Stint defaultTimeoutStint) {

    this(defaultTimeoutStint, new Stint(1, TimeUnit.SECONDS));
  }

  /**
   * Creates a map with a custom default timeout and background cleanup pulse interval.
   *
   * @param defaultTimeoutStint timeout applied to entries that do not specify their own
   * @param pulseTimeStint      how often the background worker checks for expired entries
   */
  public SelfDestructiveMap (Stint defaultTimeoutStint, Stint pulseTimeStint) {

    Thread ignitionThread;

    this.defaultTimeoutStint = defaultTimeoutStint;
    this.pulseTimeStint = pulseTimeStint;

    ignitionThread = new Thread(ignitionWorker = new IgnitionWorker());
    ignitionThread.setDaemon(true);
    ignitionThread.start();
  }

  /**
   * Returns the value associated with the specified key, or {@code null} if the key is not present.
   *
   * @param key the key to look up
   * @return the mapped value, or {@code null} if absent
   */
  public S get (K key) {

    return internalMap.get(key);
  }

  /**
   * Associates the value with the key only if the key is not already present, using the default timeout.
   *
   * @param key   key with which the value is to be associated
   * @param value value to store if the key is absent
   * @return the existing value if the key was already present, or {@code null} if the value was inserted
   */
  public S putIfAbsent (K key, S value) {

    return putIfAbsent(key, value, defaultTimeoutStint);
  }

  /**
   * Associates the value with the key only if the key is not already present, using the supplied timeout.
   *
   * @param key          key with which the value is to be associated
   * @param value        value to store if the key is absent
   * @param timeoutStint per-entry timeout; if {@code null} the default timeout is used
   * @return the existing value if the key was already present, or {@code null} if the value was inserted
   */
  public S putIfAbsent (K key, S value, Stint timeoutStint) {

    S previousValue;

    if ((previousValue = internalMap.putIfAbsent(key, value)) == null) {
      ignitionKeySet.add(new SelfDestructiveKey<>(key, (timeoutStint != null) ? timeoutStint : defaultTimeoutStint));
    }

    return previousValue;
  }

  /**
   * Stops the background cleanup worker and blocks until it has fully terminated.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the worker to stop
   */
  public void shutdown ()
    throws InterruptedException {

    ignitionWorker.shutdown();
  }

  private class IgnitionWorker implements Runnable {

    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private Thread runnableThread;

    /**
     * Signals this worker to stop and waits until it has exited.
     *
     * @throws InterruptedException if the calling thread is interrupted while waiting
     */
    public void shutdown ()
      throws InterruptedException {

      terminationLatch.countDown();

      if (runnableThread != null) {
        runnableThread.interrupt();
      }

      exitLatch.await();
    }

    /**
     * Continuously polls the expiry set at each pulse interval and invokes {@link SelfDestructive#destroy(Stint)} on any entries whose ignition time has passed.
     */
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

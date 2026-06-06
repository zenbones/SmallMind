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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.smallmind.nutsnbolts.lang.FormattedTimeoutException;
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
  private final ScheduledExecutorService ignitionExecutor;

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

    this.defaultTimeoutStint = defaultTimeoutStint;

    ignitionExecutor = Executors.newSingleThreadScheduledExecutor((runnable) -> {

      Thread thread = new Thread(runnable, "nutsnbolts-self-destructive-map");

      thread.setDaemon(true);

      return thread;
    });
    ignitionExecutor.scheduleWithFixedDelay(this::igniteExpired, pulseTimeStint.getTime(), pulseTimeStint.getTime(), pulseTimeStint.getTimeUnit());
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
   * Shuts the background cleanup executor down and waits up to three seconds for it to terminate.
   *
   * @throws InterruptedException if the calling thread is interrupted while waiting for the worker to stop
   * @throws TimeoutException     if the background worker does not terminate within three seconds
   */
  public void shutdown ()
    throws InterruptedException, TimeoutException {

    ignitionExecutor.shutdown();
    if (!ignitionExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
      throw new FormattedTimeoutException("Unable to terminate the self-destructive map in (%d, %s)", 3, TimeUnit.SECONDS.name());
    }
  }

  /**
   * Polls the expiry set once and invokes {@link SelfDestructive#destroy(Stint)} on any entries whose ignition time has passed.
   * Invoked on each pulse by the background {@link ScheduledExecutorService}.
   */
  private void igniteExpired () {

    try {

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
    } catch (Exception exception) {
      // A failed destruction must not cancel future sweeps.
    }
  }
}

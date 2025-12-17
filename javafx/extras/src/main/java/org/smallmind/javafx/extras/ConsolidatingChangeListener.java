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
package org.smallmind.javafx.extras;

import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import jfxtras.util.PlatformUtil;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Consolidates rapid bursts of {@link ChangeListener#changed(ObservableValue, Object, Object)} callbacks into
 * a single update delivered after a configurable quiet period. This is useful when reacting to noisy or high-frequency
 * change streams where intermediate states are unimportant.
 *
 * @param <T> the observed value type
 */
public class ConsolidatingChangeListener<T> implements ChangeListener<T>, Comparable<ConsolidatingChangeListener<?>> {

  private static final CountDownLatch stopLatch = new CountDownLatch(1);
  private static final ConcurrentSkipListMap<ConsolidatingKey, LooseChange<?>> LOOSE_CHANGE_MAP = new ConcurrentSkipListMap<>();
  private final ChangeListener<T> innerChangeListener;
  private final long consolidationTimeMillis;
  private int generation;

  static {

    Thread thread = new Thread(new ConsolidationWorker());

    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Creates a listener wrapper that will coalesce change notifications occurring within the supplied time window.
   *
   * @param consolidationTimeMillis the minimum quiet period in milliseconds before a change is forwarded
   * @param innerChangeListener     the listener that ultimately receives the consolidated change notification
   */
  public ConsolidatingChangeListener (long consolidationTimeMillis, ChangeListener<T> innerChangeListener) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerChangeListener = innerChangeListener;
  }

  /**
   * @return the wrapped listener that receives consolidated callbacks
   */
  private ChangeListener<T> getInnerChangeListener () {

    return innerChangeListener;
  }

  /**
   * @return the latest generation number used to identify the most recent change submitted
   */
  private synchronized int getGeneration () {

    return generation;
  }

  /**
   * Queues the observed change and schedules it to be delivered after the consolidation window expires. Any subsequent
   * changes before expiry supersede earlier ones.
   *
   * @param observableValue the observed value
   * @param initialValue    the previous value
   * @param currentValue    the new value
   */
  @Override
  public synchronized final void changed (ObservableValue<? extends T> observableValue, T initialValue, T currentValue) {

    LOOSE_CHANGE_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), new LooseChange<>(observableValue, initialValue, currentValue));
  }

  /**
   * Compares listeners by identity to provide ordering within the consolidation map.
   *
   * @param listener another listener to compare
   * @return a positive, negative or zero result based on the instance hash codes
   */
  @Override
  public int compareTo (ConsolidatingChangeListener<?> listener) {

    return hashCode() - listener.hashCode();
  }

  /**
   * Drains expired entries from the queue and delivers the last change for each listener generation on the JavaFX thread.
   */
  private static class ConsolidationWorker implements Runnable {

    /**
     * Polls for expired entries until signalled to stop, dispatching consolidated changes for each listener generation.
     */
    @Override
    public void run () {

      try {
        while (!stopLatch.await(50, TimeUnit.MILLISECONDS)) {

          NavigableMap<ConsolidatingKey, LooseChange<?>> expiredKeyMap;

          if (!(expiredKeyMap = LOOSE_CHANGE_MAP.headMap(new ConsolidatingKey())).isEmpty()) {

            Map.Entry<ConsolidatingKey, LooseChange<?>> entry;

            while ((entry = expiredKeyMap.pollFirstEntry()) != null) {
              synchronized (entry.getKey().getListener()) {
                if (entry.getKey().getGeneration() == entry.getKey().getListener().getGeneration()) {

                  final ConsolidatingKey key = entry.getKey();
                  final LooseChange<?> change = entry.getValue();

                  PlatformUtil.runAndWait(new Runnable() {

                    @Override
                    public void run () {

                      key.getListener().getInnerChangeListener().changed(change.observableValue(), change.initialValue(), change.currentValue());
                    }
                  });
                }
              }
            }
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ConsolidatingChangeListener.class).error(interruptedException);
      }
    }
  }

  /**
   * Key used to order pending changes by expiration time and listener identity.
   *
   * @param <U> the observed value type
   */
  private static class ConsolidatingKey<U> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingChangeListener<U> listener;
    private final long expiration;
    private final int generation;

    /**
     * Constructs a sentinel key with no listener used to query the head map for expired entries.
     */
    private ConsolidatingKey () {

      this(null, 0, 0);
    }

    /**
     * Constructs a key representing a scheduled change.
     *
     * @param listener                the listener associated with the change
     * @param generation              the generation number of the change
     * @param consolidationTimeMillis the delay before the change should be emitted
     */
    private ConsolidatingKey (ConsolidatingChangeListener<U> listener, int generation, long consolidationTimeMillis) {

      this.listener = listener;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    /**
     * @return the listener that will receive the consolidated change
     */
    private ConsolidatingChangeListener<?> getListener () {

      return listener;
    }

    /**
     * @return the generation number for the scheduled change
     */
    private int getGeneration () {

      return generation;
    }

    /**
     * @return the epoch time in milliseconds when this change expires
     */
    private long getExpiration () {

      return expiration;
    }

    /**
     * Orders keys by expiration time, then by listener identity to maintain a deterministic ordering.
     *
     * @param key another key
     * @return comparison result suitable for sorted map usage
     */
    @Override
    public int compareTo (ConsolidatingKey key) {

      int comparison;

      if ((comparison = Long.compare(expiration, key.getExpiration())) == 0) {

        return (listener == null) ? ((key.getListener() == null) ? 0 : -1) : ((key.getListener() == null) ? 1 : listener.compareTo(key.getListener()));
      }

      return comparison;
    }
  }

  /**
   * Encapsulates the change state captured for consolidation.
   *
   * @param <U>             the observed value type
   * @param observableValue the observed value source
   * @param initialValue    the previous value
   * @param currentValue    the new value
   */
  private record LooseChange<U>(ObservableValue<? extends U> observableValue, U initialValue, U currentValue) {

  }
}

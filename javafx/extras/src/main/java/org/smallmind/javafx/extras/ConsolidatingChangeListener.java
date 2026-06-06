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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import jfxtras.util.PlatformUtil;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * A debouncing wrapper around a {@link ChangeListener} that consolidates rapid bursts of change
 * notifications into a single delivery after a configurable quiet period. While the quiet window
 * is open each new change supersedes the previous one so only the most recent state is forwarded.
 * Delivery is always dispatched on the JavaFX application thread. A shared single-thread
 * {@link ScheduledExecutorService} checks for expired entries on a daemon thread every 250 ms.
 *
 * @param <T> the type of the observed value
 */
public class ConsolidatingChangeListener<T> implements ChangeListener<T>, Comparable<ConsolidatingChangeListener<?>> {

  private static final ConcurrentSkipListMap<ConsolidatingKey, LooseChange<?>> LOOSE_CHANGE_MAP = new ConcurrentSkipListMap<>();
  private static final ScheduledExecutorService CONSOLIDATION_EXECUTOR = Executors.newSingleThreadScheduledExecutor((runnable) -> {

    Thread thread = new Thread(runnable, "javafx-consolidating-change");

    thread.setDaemon(true);

    return thread;
  });
  private final ChangeListener<T> innerChangeListener;
  private final long consolidationTimeMillis;
  private int generation;

  static {
    CONSOLIDATION_EXECUTOR.scheduleWithFixedDelay(ConsolidatingChangeListener::dispatchExpired, 250, 250, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates a listener that coalesces notifications arriving within the given window and forwards
   * only the last one to {@code innerChangeListener}.
   *
   * @param consolidationTimeMillis quiet-period length in milliseconds; notifications received
   *                                within this window after the first are suppressed
   * @param innerChangeListener     the delegate that receives the consolidated notification;
   *                                must not be {@code null}
   */
  public ConsolidatingChangeListener (long consolidationTimeMillis, ChangeListener<T> innerChangeListener) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerChangeListener = innerChangeListener;
  }

  /**
   * Drains expired entries from the pending-change map once and dispatches the latest generation for
   * each listener on the JavaFX application thread. For each expired entry whose generation matches
   * the listener's current generation, the change is dispatched synchronously on the JavaFX
   * application thread. Any failure of the pass is logged so that it cannot cancel future runs.
   * Invoked every 250 ms by the shared {@link ScheduledExecutorService}.
   */
  private static void dispatchExpired () {

    try {

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
    } catch (Exception exception) {
      LoggerManager.getLogger(ConsolidatingChangeListener.class).error(exception);
    }
  }

  /**
   * Returns the delegate listener that ultimately receives consolidated change notifications.
   *
   * @return the inner listener; never {@code null}
   */
  private ChangeListener<T> getInnerChangeListener () {

    return innerChangeListener;
  }

  /**
   * Returns the current generation counter. The counter is incremented each time {@link #changed}
   * is called; the scheduled {@link #dispatchExpired} pass uses it to detect whether a queued entry
   * is still the latest.
   *
   * @return the current generation number
   */
  private synchronized int getGeneration () {

    return generation;
  }

  /**
   * Queues the observed change for deferred delivery. Any previously queued change for this
   * listener is superseded. The method is safe to call from any thread.
   *
   * @param observableValue the source observable
   * @param initialValue    the previous value before the change
   * @param currentValue    the new value after the change
   */
  @Override
  public synchronized final void changed (ObservableValue<? extends T> observableValue, T initialValue, T currentValue) {

    LOOSE_CHANGE_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), new LooseChange<>(observableValue, initialValue, currentValue));
  }

  /**
   * Compares this listener to another by identity hash code to produce a consistent ordering
   * within the shared skip-list map.
   *
   * @param listener the listener to compare against; must not be {@code null}
   * @return a negative, zero, or positive integer based on hash code difference
   */
  @Override
  public int compareTo (ConsolidatingChangeListener<?> listener) {

    return hashCode() - listener.hashCode();
  }

  /**
   * Ordering key used to schedule and retrieve pending change notifications in the shared
   * skip-list map. Keys are ordered first by expiration time, then by listener identity.
   *
   * @param <U> the observed value type of the associated listener
   */
  private static class ConsolidatingKey<U> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingChangeListener<U> listener;
    private final long expiration;
    private final int generation;

    /**
     * Creates a sentinel key whose expiration is the current time, used as the exclusive upper bound
     * argument when querying the map for entries whose quiet window has elapsed.
     */
    private ConsolidatingKey () {

      this(null, 0, 0);
    }

    /**
     * Creates a key representing a scheduled change notification.
     *
     * @param listener                the listener that will receive the notification
     * @param generation              monotonically increasing identifier for this notification
     * @param consolidationTimeMillis the delay in milliseconds before the notification may be dispatched
     */
    private ConsolidatingKey (ConsolidatingChangeListener<U> listener, int generation, long consolidationTimeMillis) {

      this.listener = listener;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    /**
     * Returns the listener associated with this scheduled change.
     *
     * @return the owning listener, or {@code null} for a sentinel key
     */
    private ConsolidatingChangeListener<?> getListener () {

      return listener;
    }

    /**
     * Returns the generation number that identifies this particular change within the listener's
     * notification sequence.
     *
     * @return the generation counter value
     */
    private int getGeneration () {

      return generation;
    }

    /**
     * Returns the wall-clock time in milliseconds at which this entry becomes eligible for dispatch.
     *
     * @return expiration epoch time in milliseconds
     */
    private long getExpiration () {

      return expiration;
    }

    /**
     * Orders keys by expiration time ascending. When expiration times are equal, sentinel keys
     * (null listener) sort before live keys, and live keys are ordered by listener identity.
     *
     * @param key the key to compare against; must not be {@code null}
     * @return a negative, zero, or positive integer
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
   * Immutable snapshot of a single change notification held in the queue until the quiet window expires.
   *
   * @param <U>             the observed value type
   * @param observableValue the source observable that emitted the change
   * @param initialValue    the value prior to the change
   * @param currentValue    the value after the change
   */
  private record LooseChange<U>(ObservableValue<? extends U> observableValue, U initialValue, U currentValue) {

  }
}

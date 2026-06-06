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
import javafx.event.Event;
import javafx.event.EventHandler;
import jfxtras.util.PlatformUtil;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * A debouncing wrapper around an {@link EventHandler} that consolidates rapid bursts of events
 * into a single dispatch after a configurable quiet period. Each new event received within the
 * quiet window supersedes the previous one so only the most recent event is forwarded. Dispatch
 * always occurs on the JavaFX application thread. A shared single-thread {@link ScheduledExecutorService}
 * checks for expired entries on a daemon thread every 250 ms.
 *
 * @param <T> the type of event handled
 */
public class ConsolidatingEventHandler<T extends Event> implements EventHandler<T>, Comparable<ConsolidatingEventHandler<?>> {

  private static final ConcurrentSkipListMap<ConsolidatingKey, Event> LOOSE_EVENT_MAP = new ConcurrentSkipListMap<>();
  private static final ScheduledExecutorService CONSOLIDATION_EXECUTOR = Executors.newSingleThreadScheduledExecutor((runnable) -> {

    Thread thread = new Thread(runnable, "javafx-consolidating-event");

    thread.setDaemon(true);

    return thread;
  });
  private final EventHandler<T> innerEventHandler;
  private final long consolidationTimeMillis;
  private int generation;

  static {
    CONSOLIDATION_EXECUTOR.scheduleWithFixedDelay(ConsolidatingEventHandler::dispatchExpired, 250, 250, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates an event handler that delays delivery by {@code consolidationTimeMillis} and discards
   * intermediate events, ultimately forwarding only the last received event to {@code innerEventHandler}.
   *
   * @param consolidationTimeMillis the quiet-period length in milliseconds; events received within
   *                                this window after the first are suppressed
   * @param innerEventHandler       the delegate that receives the consolidated event;
   *                                must not be {@code null}
   */
  public ConsolidatingEventHandler (long consolidationTimeMillis, EventHandler<T> innerEventHandler) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerEventHandler = innerEventHandler;
  }

  /**
   * Drains expired entries from the pending-event map once and dispatches the latest generation for
   * each handler on the JavaFX application thread. For each expired entry whose generation matches
   * the handler's current generation, the event is dispatched synchronously on the JavaFX
   * application thread. Any failure of the pass is logged so that it cannot cancel future runs.
   * Invoked every 250 ms by the shared {@link ScheduledExecutorService}.
   */
  private static void dispatchExpired () {

    try {

      NavigableMap<ConsolidatingKey, Event> expiredKeyMap;

      if (!(expiredKeyMap = LOOSE_EVENT_MAP.headMap(new ConsolidatingKey())).isEmpty()) {

        Map.Entry<ConsolidatingKey, Event> entry;

        while ((entry = expiredKeyMap.pollFirstEntry()) != null) {
          synchronized (entry.getKey().getEventHandler()) {
            if (entry.getKey().getGeneration() == entry.getKey().getEventHandler().getGeneration()) {

              final ConsolidatingKey key = entry.getKey();
              final Event event = entry.getValue();

              PlatformUtil.runAndWait(new Runnable() {

                @Override
                public void run () {

                  key.getEventHandler().getInnerEventHandler().handle(event);
                }
              });
            }
          }
        }
      }
    } catch (Exception exception) {
      LoggerManager.getLogger(ConsolidatingEventHandler.class).error(exception);
    }
  }

  /**
   * Returns the delegate handler that ultimately receives consolidated event dispatches.
   *
   * @return the inner handler; never {@code null}
   */
  private EventHandler<T> getInnerEventHandler () {

    return innerEventHandler;
  }

  /**
   * Returns the current generation counter. Incremented each time {@link #handle} is called; the
   * scheduled {@link #dispatchExpired} pass uses it to detect stale queue entries.
   *
   * @return the current generation number
   */
  private synchronized int getGeneration () {

    return generation;
  }

  /**
   * Queues the event for deferred delivery, superseding any previously queued event for this
   * handler. Safe to call from any thread.
   *
   * @param event the event to queue; must not be {@code null}
   */
  @Override
  public synchronized void handle (T event) {

    LOOSE_EVENT_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), event);
  }

  /**
   * Compares this handler to another by identity hash code to produce a consistent ordering
   * within the shared skip-list map.
   *
   * @param handler the handler to compare against; must not be {@code null}
   * @return a negative, zero, or positive integer based on hash code difference
   */
  @Override
  public int compareTo (ConsolidatingEventHandler<?> handler) {

    return hashCode() - handler.hashCode();
  }

  /**
   * Ordering key used to schedule and retrieve pending event dispatches in the shared skip-list
   * map. Keys are ordered first by expiration time, then by handler identity.
   *
   * @param <U> the event type of the associated handler
   */
  private static class ConsolidatingKey<U extends Event> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingEventHandler<U> handler;
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
     * Creates a key representing a scheduled event dispatch.
     *
     * @param handler                 the handler that will receive the event
     * @param generation              monotonically increasing identifier for this dispatch
     * @param consolidationTimeMillis the delay in milliseconds before the event may be dispatched
     */
    private ConsolidatingKey (ConsolidatingEventHandler<U> handler, int generation, long consolidationTimeMillis) {

      this.handler = handler;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    /**
     * Returns the handler associated with this scheduled event dispatch.
     *
     * @return the owning handler, or {@code null} for a sentinel key
     */
    private ConsolidatingEventHandler<?> getEventHandler () {

      return handler;
    }

    /**
     * Returns the generation number that identifies this particular event within the handler's
     * dispatch sequence.
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
     * (null handler) sort before live keys, and live keys are ordered by handler identity.
     *
     * @param key the key to compare against; must not be {@code null}
     * @return a negative, zero, or positive integer
     */
    @Override
    public int compareTo (ConsolidatingKey key) {

      int comparison;

      if ((comparison = Long.compare(expiration, key.getExpiration())) == 0) {

        return (handler == null) ? ((key.getEventHandler() == null) ? 0 : -1) : ((key.getEventHandler() == null) ? 1 : handler.compareTo(key.getEventHandler()));
      }

      return comparison;
    }
  }
}

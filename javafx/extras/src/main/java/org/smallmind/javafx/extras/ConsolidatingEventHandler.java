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
import javafx.event.Event;
import javafx.event.EventHandler;
import jfxtras.util.PlatformUtil;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Consolidates rapid {@link EventHandler#handle(Event)} invocations into a single dispatch after a quiet period.
 * This allows filtering high-frequency events while still processing the most recent occurrence.
 *
 * @param <T> the event type handled
 */
public class ConsolidatingEventHandler<T extends Event> implements EventHandler<T>, Comparable<ConsolidatingEventHandler<?>> {

  private static final CountDownLatch stopLatch = new CountDownLatch(1);
  private static final ConcurrentSkipListMap<ConsolidatingKey, Event> LOOSE_EVENT_MAP = new ConcurrentSkipListMap<>();
  private final EventHandler<T> innerEventHandler;
  private final long consolidationTimeMillis;
  private int generation;

  static {

    Thread thread = new Thread(new ConsolidationWorker());

    thread.setDaemon(true);
    thread.start();
  }

  /**
   * Creates an event handler that delays and consolidates event notifications.
   *
   * @param consolidationTimeMillis the quiet period in milliseconds used to coalesce events
   * @param innerEventHandler       the handler that will receive the consolidated event
   */
  public ConsolidatingEventHandler (long consolidationTimeMillis, EventHandler<T> innerEventHandler) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerEventHandler = innerEventHandler;
  }

  /**
   * @return the wrapped event handler to invoke after consolidation
   */
  private EventHandler<T> getInnerEventHandler () {

    return innerEventHandler;
  }

  /**
   * @return the current generation counter used to identify the latest queued event
   */
  private synchronized int getGeneration () {

    return generation;
  }

  /**
   * Enqueues the event for delivery after the consolidation window, replacing any previously queued event.
   *
   * @param event the event that occurred
   */
  @Override
  public synchronized void handle (T event) {

    LOOSE_EVENT_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), event);
  }

  /**
   * Compares handlers by identity for ordering in the consolidation map.
   *
   * @param handler the handler to compare to
   * @return comparison result based on instance hash codes
   */
  @Override
  public int compareTo (ConsolidatingEventHandler<?> handler) {

    return hashCode() - handler.hashCode();
  }

  /**
   * Drains expired events and dispatches the latest generation on the JavaFX thread.
   */
  private static class ConsolidationWorker implements Runnable {

    /**
     * Polls periodically for expired events until stopped and dispatches them.
     */
    @Override
    public void run () {

      try {
        while (!stopLatch.await(50, TimeUnit.MILLISECONDS)) {

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
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ConsolidatingChangeListener.class).error(interruptedException);
      }
    }
  }

  /**
   * Key used to order pending events by expiration and handler identity.
   *
   * @param <U> the event type
   */
  private static class ConsolidatingKey<U extends Event> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingEventHandler<U> handler;
    private final long expiration;
    private final int generation;

    /**
     * Creates a sentinel key for head map searches.
     */
    private ConsolidatingKey () {

      this(null, 0, 0);
    }

    /**
     * Constructs a key representing a scheduled event notification.
     *
     * @param handler                 the handler that will receive the event
     * @param generation              the generation of the queued event
     * @param consolidationTimeMillis the delay before dispatching
     */
    private ConsolidatingKey (ConsolidatingEventHandler<U> handler, int generation, long consolidationTimeMillis) {

      this.handler = handler;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    /**
     * @return the handler to dispatch to
     */
    private ConsolidatingEventHandler<?> getEventHandler () {

      return handler;
    }

    /**
     * @return the generation identifier
     */
    private int getGeneration () {

      return generation;
    }

    /**
     * @return expiration time in milliseconds
     */
    private long getExpiration () {

      return expiration;
    }

    /**
     * Orders keys by expiration time then handler identity to ensure consistent ordering.
     *
     * @param key another key
     * @return comparison result
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

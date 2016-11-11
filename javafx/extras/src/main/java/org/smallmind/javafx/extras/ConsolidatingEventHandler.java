/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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

  public ConsolidatingEventHandler (long consolidationTimeMillis, EventHandler<T> innerEventHandler) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerEventHandler = innerEventHandler;
  }

  private EventHandler<T> getInnerEventHandler () {

    return innerEventHandler;
  }

  private synchronized int getGeneration () {

    return generation;
  }

  @Override
  public synchronized final void handle (T event) {

    LOOSE_EVENT_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), event);
  }

  @Override
  public int compareTo (ConsolidatingEventHandler<?> handler) {

    return hashCode() - handler.hashCode();
  }

  private static class ConsolidationWorker implements Runnable {

    @Override
    protected void finalize () {

      stopLatch.countDown();
    }

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

  private static class ConsolidatingKey<U extends Event> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingEventHandler<U> handler;
    private final long expiration;
    private final int generation;

    private ConsolidatingKey () {

      this(null, 0, 0);
    }

    private ConsolidatingKey (ConsolidatingEventHandler<U> handler, int generation, long consolidationTimeMillis) {

      this.handler = handler;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    private ConsolidatingEventHandler<?> getEventHandler () {

      return handler;
    }

    private int getGeneration () {

      return generation;
    }

    private long getExpiration () {

      return expiration;
    }

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

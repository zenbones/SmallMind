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
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import jfxtras.util.PlatformUtil;
import org.smallmind.scribe.pen.LoggerManager;

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

  public ConsolidatingChangeListener (long consolidationTimeMillis, ChangeListener<T> innerChangeListener) {

    this.consolidationTimeMillis = consolidationTimeMillis;
    this.innerChangeListener = innerChangeListener;
  }

  private ChangeListener<T> getInnerChangeListener () {

    return innerChangeListener;
  }

  private synchronized int getGeneration () {

    return generation;
  }

  @Override
  public synchronized final void changed (ObservableValue<? extends T> observableValue, T initialValue, T currentValue) {

    LOOSE_CHANGE_MAP.put(new ConsolidatingKey<>(this, ++generation, consolidationTimeMillis), new LooseChange<>(observableValue, initialValue, currentValue));
  }

  @Override
  public int compareTo (ConsolidatingChangeListener<?> listener) {

    return hashCode() - listener.hashCode();
  }

  @Override
  protected void finalize () {

    stopLatch.countDown();
  }

  private static class ConsolidationWorker implements Runnable {

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

                      key.getListener().getInnerChangeListener().changed(change.getObservableValue(), change.getInitialValue(), change.getCurrentValue());
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

  private static class ConsolidatingKey<U> implements Comparable<ConsolidatingKey<U>> {

    private final ConsolidatingChangeListener<U> listener;
    private final long expiration;
    private final int generation;

    private ConsolidatingKey () {

      this(null, 0, 0);
    }

    private ConsolidatingKey (ConsolidatingChangeListener<U> listener, int generation, long consolidationTimeMillis) {

      this.listener = listener;
      this.generation = generation;

      expiration = System.currentTimeMillis() + consolidationTimeMillis;
    }

    private ConsolidatingChangeListener<?> getListener () {

      return listener;
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

        return (listener == null) ? ((key.getListener() == null) ? 0 : -1) : ((key.getListener() == null) ? 1 : listener.compareTo(key.getListener()));
      }

      return comparison;
    }
  }

  private static class LooseChange<U> {

    private final ObservableValue<? extends U> observableValue;
    private final U initialValue;
    private final U currentValue;

    private LooseChange (ObservableValue<? extends U> observableValue, U initialValue, U currentValue) {

      this.observableValue = observableValue;
      this.initialValue = initialValue;
      this.currentValue = currentValue;
    }

    private ObservableValue<? extends U> getObservableValue () {

      return observableValue;
    }

    private U getInitialValue () {

      return initialValue;
    }

    private U getCurrentValue () {

      return currentValue;
    }
  }
}

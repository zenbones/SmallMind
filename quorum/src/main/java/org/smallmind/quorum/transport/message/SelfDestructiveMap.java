/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class SelfDestructiveMap<K extends Comparable<K>, V extends SelfDestructive> {

  private final ConcurrentHashMap<K, V> internalMap = new ConcurrentHashMap<K, V>();
  private final ConcurrentSkipListSet<IgnitionKey<K>> ignitionKeySet = new ConcurrentSkipListSet<IgnitionKey<K>>();
  private IgnitionWorker ignitionWorker;
  private final int timeoutSeconds;

  public SelfDestructiveMap (int timeoutSeconds) {

    Thread ignitionThread;

    this.timeoutSeconds = timeoutSeconds;

    ignitionThread = new Thread(ignitionWorker = new IgnitionWorker());
    ignitionThread.setDaemon(true);
    ignitionThread.start();
  }

  public V putIfAbsent (K key, V value) {

    V previousValue;

    if ((previousValue = internalMap.putIfAbsent(key, value)) == null) {
      ignitionKeySet.add(new IgnitionKey<K>(key, System.currentTimeMillis() + (timeoutSeconds * 1000)));
    }

    return previousValue;
  }

  public V remove (K key) {

    return internalMap.remove(key);
  }

  public void shutdown ()
    throws InterruptedException {

    ignitionWorker.shutdown();
  }

  private class IgnitionWorker implements Runnable {

    private final CountDownLatch terminationLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);

    public void shutdown ()
      throws InterruptedException {

      terminationLatch.countDown();
      exitLatch.await();
    }

    @Override
    public void run () {

      try {
        while (!terminationLatch.await(1, TimeUnit.SECONDS)) {

          IgnitionKey<K> firstKey;
          long now = System.currentTimeMillis();

          while (((firstKey = ignitionKeySet.first()) != null) && (firstKey.getIgnitionTime() <= now)) {

            SelfDestructive selfDestructive;

            if ((selfDestructive = internalMap.remove(firstKey.getMapKey())) != null) {
              selfDestructive.destroy();
            }
          }
        }
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(SelfDestructiveMap.class).error(interruptedException);
      }

      exitLatch.countDown();
    }
  }
}

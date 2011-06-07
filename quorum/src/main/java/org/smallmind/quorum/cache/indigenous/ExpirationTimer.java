/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.quorum.cache.indigenous;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class ExpirationTimer<K> implements Runnable {

   /*
   * Marked as transient so that Terracotta will not attempt to share these fields, which, as
   * they're not marked as roots, shouldn't be a problem, but Terracotta traverses the graph of
   * this object due to the shared processing field, which is a root, and complains
   */
   private transient CountDownLatch terminationLatch;
   private transient CountDownLatch exitLatch;

   private AbstractCache<K, ?, ?> cache;
   private AtomicBoolean processing = new AtomicBoolean(false);
   private AtomicBoolean finished = new AtomicBoolean(false);
   private int expirationTimerTickSeconds;

   public ExpirationTimer (AbstractCache<K, ?, ?> cache, int expirationTimerTickSeconds) {

      this.cache = cache;
      this.expirationTimerTickSeconds = expirationTimerTickSeconds;

      terminationLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public boolean isProcessing () {

      return processing.get();
   }

   public void finish () {

      if (finished.compareAndSet(false, true)) {
         terminationLatch.countDown();
      }

      try {
         exitLatch.await();
      }
      catch (InterruptedException i) {
      }
   }

   public void run () {

      try {
         while (!finished.get()) {
            terminationLatch.await(expirationTimerTickSeconds, TimeUnit.SECONDS);

            if (!finished.get()) {
               /**
                * This weirdness is driven by Terracotta - The cache internals are shared across a cluster,
                * so there may be many threads running this code, but we only need one of them to actually
                * execute, so the code gets guarded by a shared atomic boolean
                */
               if (processing.compareAndSet(false, true)) {
                  try {
                     for (K key : new IterableIterator<K>(cache.getKeyIterator())) {
//TODO:
                        /*
                        cache.executeLockedCallback(new LockedCallback<K, Void>() {

                           public K getKey () {

                              return key;
                           }

                           public Void execute () {

                              cache.retrieveEntry(key);

                              return null;
                           }
                        });
                        */
                     }
                  }
                  finally {
                     processing.set(false);
                  }
               }
            }
         }
      }
      catch (InterruptedException i) {
         finished.set(true);
      }
      finally {
         exitLatch.countDown();
      }
   }
}

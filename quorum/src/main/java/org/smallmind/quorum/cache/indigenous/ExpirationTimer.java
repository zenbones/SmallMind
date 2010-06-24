package org.smallmind.quorum.cache.indigenous;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.nutsnbolts.util.IterableIterator;

public class ExpirationTimer<K> implements Runnable {

   /*
   * Marked as transient so that Terracotta will not attempt to share these fields, which, as
   * they're not marked as roots, shouldn't be a problem, but Terracotta traverses the graph of
   * this object due to the shared processing field, which is a root, and comnplains
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
                * This wierdness is driven by Terracotta - The cache internals are shared across a cluster,
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

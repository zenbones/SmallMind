package org.smallmind.quorum.pool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class DeconstructionFuse implements Runnable {

   private DeconstructionWorker deconstructionWorker;
   private CountDownLatch abortedLatch;
   private AtomicBoolean aborted = new AtomicBoolean(false);

   public void setDeconstructionLatch (DeconstructionWorker deconstructionWorker) {

      this.deconstructionWorker = deconstructionWorker;

      abortedLatch = new CountDownLatch(1);
   }

   public abstract void free ();

   public abstract void serve ();

   public void sleep (int sleepSeconds)
      throws InterruptedException {

      abortedLatch.await(sleepSeconds, TimeUnit.SECONDS);
   }

   public boolean hasBeenAborted () {

      return aborted.get();
   }

   public void abort () {

      if (aborted.compareAndSet(false, true)) {
         abortedLatch.countDown();
      }
   }

   public void ignite (boolean forced) {

      deconstructionWorker.ignite(forced);
   }
}

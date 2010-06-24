package org.smallmind.quorum.pool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MaxIdleTimeDeconstructionFuse extends DeconstructionFuse {

   private CyclicBarrier freeBarrier;
   private CyclicBarrier serveBarrier;
   private int maxIdleTimeSeconds;

   public MaxIdleTimeDeconstructionFuse (int maxIdleTimeSeconds) {

      this.maxIdleTimeSeconds = maxIdleTimeSeconds;

      freeBarrier = new CyclicBarrier(2);
      serveBarrier = new CyclicBarrier(2);
   }

   public void free () {

      try {
         freeBarrier.await();
      }
      catch (Exception exception) {
      }
   }

   public void serve () {

      try {
         serveBarrier.await();
      }
      catch (Exception exception) {
      }
   }

   public void run () {

      try {
         while (!hasBeenAborted()) {
            freeBarrier.await();
            freeBarrier.reset();

            serveBarrier.await(maxIdleTimeSeconds, TimeUnit.SECONDS);
            serveBarrier.reset();
         }
      }
      catch (TimeoutException timeoutException) {
         ignite(false);
      }
      catch (Exception exception) {
      }

      freeBarrier.reset();
      serveBarrier.reset();
   }
}
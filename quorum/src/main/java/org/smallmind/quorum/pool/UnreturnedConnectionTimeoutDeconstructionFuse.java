package org.smallmind.quorum.pool;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class UnreturnedConnectionTimeoutDeconstructionFuse extends DeconstructionFuse {

   private CyclicBarrier freeBarrier;
   private CyclicBarrier serveBarrier;
   private int unreturnedConnectionTimeoutSeconds;

   public UnreturnedConnectionTimeoutDeconstructionFuse (int unreturnedConnectionTimeoutSeconds) {

      this.unreturnedConnectionTimeoutSeconds = unreturnedConnectionTimeoutSeconds;

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
            serveBarrier.await();
            serveBarrier.reset();

            freeBarrier.await(unreturnedConnectionTimeoutSeconds, TimeUnit.SECONDS);
            freeBarrier.reset();
         }
      }
      catch (TimeoutException timeoutException) {
         ignite(true);
      }
      catch (Exception exception) {
      }

      freeBarrier.reset();
      serveBarrier.reset();
   }
}
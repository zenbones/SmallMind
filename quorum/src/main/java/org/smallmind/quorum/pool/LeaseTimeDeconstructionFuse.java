package org.smallmind.quorum.pool;

public class LeaseTimeDeconstructionFuse extends DeconstructionFuse {

   private int leaseTimeSeconds;

   public LeaseTimeDeconstructionFuse (int leaseTimeSeconds) {

      this.leaseTimeSeconds = leaseTimeSeconds;
   }

   public void free () {
   }

   public void serve () {
   }

   public void run () {

      try {
         sleep(leaseTimeSeconds);
         if (!hasBeenAborted()) {
            ignite(false);
         }
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }
   }
}

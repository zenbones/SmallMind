package org.smallmind.cloud.transport.remote;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.activation.ActivationException;
import java.rmi.activation.ActivationGroup;
import java.rmi.activation.ActivationSystem;
import java.rmi.registry.LocateRegistry;

public class RemoteUtilities {

   private static ActivationSystem activationSystem;

   public static synchronized void startRMIRegistry ()
      throws RemoteException {

      try {
         LocateRegistry.createRegistry(1099);
      }
      catch (RemoteException r) {
         LocateRegistry.getRegistry(1099);
      }
   }

   public static synchronized void startRMIDaemon (int maxAttempts, long sleepTime)
      throws IOException {

      int retryCount = 0;

      if (activationSystem == null) {
         do {
            try {
               activationSystem = ActivationGroup.getSystem();
            }
            catch (ActivationException a) {
               if (retryCount++ == maxAttempts) {
                  throw new IOException("Unable to bind the rmi daemon");
               }

               Runtime.getRuntime().exec("rmid");

               try {
                  Thread.sleep(sleepTime);
               }
               catch (InterruptedException i) {
               }
            }
         } while (activationSystem == null);
      }
   }

   public static void stopRMIDaemon ()
      throws RemoteException {

      if (activationSystem != null) {
         activationSystem.shutdown();
         activationSystem = null;
      }
   }

}

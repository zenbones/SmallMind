/*
 * Copyright (c) 2007, 2008, 2009, 2010 David Berkman
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

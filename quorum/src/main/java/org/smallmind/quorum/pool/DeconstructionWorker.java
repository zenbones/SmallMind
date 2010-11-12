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
package org.smallmind.quorum.pool;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class DeconstructionWorker implements java.lang.Runnable {

   private final ConnectionPin connectionPin;

   private ConnectionPool connectionPool;
   private CountDownLatch deconstructionLatch;
   private CountDownLatch exitLatch;
   private List<DeconstructionFuse> fuseList;
   private HashMap<DeconstructionFuse, Thread> fuseThreadMap;
   private AtomicBoolean terminated = new AtomicBoolean(false);
   private boolean forced = false;
   private boolean aborted = false;

   public DeconstructionWorker (ConnectionPool connectionPool, ConnectionPin connectionPin, List<DeconstructionFuse> fuseList) {

      Thread fuseThread;

      this.connectionPool = connectionPool;
      this.connectionPin = connectionPin;
      this.fuseList = fuseList;

      deconstructionLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);

      fuseThreadMap = new HashMap<DeconstructionFuse, Thread>();
      for (DeconstructionFuse deconstructionFuse : fuseList) {
         deconstructionFuse.setDeconstructionLatch(this);
         fuseThread = new Thread(deconstructionFuse);
         fuseThread.setDaemon(true);
         fuseThreadMap.put(deconstructionFuse, fuseThread);

         fuseThread.start();
      }
   }

   public void free () {

      for (DeconstructionFuse fuse : fuseList) {
         fuse.free();
      }
   }

   public void serve () {

      for (DeconstructionFuse fuse : fuseList) {
         fuse.serve();
      }
   }

   public void ignite (boolean forced) {

      if (terminated.compareAndSet(false, true)) {
         if (forced) {
            this.forced = true;
         }

         deconstructionLatch.countDown();
      }

      try {
         exitLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }
   }

   public void abort () {

      if (terminated.compareAndSet(false, true)) {
         aborted = true;
         deconstructionLatch.countDown();
      }

      try {
         exitLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }
   }

   public void run () {

      boolean deconstructed = false;

      try {
         deconstructionLatch.await();
      }
      catch (InterruptedException interruptedException) {
         ConnectionPoolManager.logError(interruptedException);
      }

      for (DeconstructionFuse deconstructionFuse : fuseThreadMap.keySet()) {
         deconstructionFuse.abort();
      }

      try {
         if (!aborted) {
            synchronized (connectionPin) {
               connectionPin.decommission();
            }

            while (!deconstructed) {
               synchronized (connectionPin) {
                  if (forced || connectionPin.isFree()) {
                     try {
                        connectionPin.close();
                     }
                     catch (Exception exception) {
                        ConnectionPoolManager.logError(exception);
                     }
                  }

                  if (connectionPin.isClosed()) {
                     deconstructed = true;
                  }
               }

               if (deconstructed) {
                  try {
                     connectionPool.removePin(connectionPin);
                  }
                  catch (Exception exception) {
                     ConnectionPoolManager.logError(exception);
                  }
               }
               else {
                  try {
                     Thread.sleep(100);
                  }
                  catch (InterruptedException interruptedException) {
                     ConnectionPoolManager.logError(interruptedException);
                  }
               }
            }
         }
      }
      finally {
         exitLatch.countDown();
      }
   }
}

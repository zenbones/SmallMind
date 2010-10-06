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
package org.smallmind.cloud.cluster;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.smallmind.scribe.pen.LoggerManager;

public class ClusterUpdateTimer implements Runnable {

   private CountDownLatch exitLatch;
   private CountDownLatch finishLatch;
   private ClusterHub clusterHub;
   private long pulseTime;

   public ClusterUpdateTimer (ClusterHub clusterHub, int updateInterval) {

      this.clusterHub = clusterHub;

      pulseTime = updateInterval * 1000;

      finishLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public void finish ()
      throws InterruptedException {

      finishLatch.countDown();
      exitLatch.await();
   }

   public void run () {

      try {
         do {
            clusterHub.fireStatusUpdate(clusterHub.getClientClusterInstances());
         } while (!finishLatch.await(pulseTime, TimeUnit.MILLISECONDS));
      }
      catch (InterruptedException interruptedException) {
         finishLatch.countDown();
         LoggerManager.getLogger(ClusterUpdateTimer.class).error(interruptedException);
      }
      finally {
         exitLatch.countDown();
      }
   }
}

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

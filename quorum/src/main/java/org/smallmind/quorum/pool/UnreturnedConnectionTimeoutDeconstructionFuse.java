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
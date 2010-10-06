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
package org.smallmind.swing.progress;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ProgressTimer implements Runnable {

   private CountDownLatch exitLatch;
   private CountDownLatch pulseLatch;
   private AtomicBoolean finished = new AtomicBoolean(false);
   private ProgressPanel progressPanel;
   private long pulseTime;

   public ProgressTimer (ProgressPanel progressPanel, long pulseTime) {

      this.progressPanel = progressPanel;
      this.pulseTime = pulseTime;

      pulseLatch = new CountDownLatch(1);
      exitLatch = new CountDownLatch(1);
   }

   public void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
         pulseLatch.countDown();
      }

      exitLatch.await();
   }

   public void run () {

      try {
         while (!finished.get()) {
            progressPanel.setProgress();

            try {
               pulseLatch.await(pulseTime, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException interruptedException) {
               LoggerManager.getLogger(ProgressTimer.class).error(interruptedException);
            }
         }
      }
      finally {
         exitLatch.countDown();
      }
   }
}

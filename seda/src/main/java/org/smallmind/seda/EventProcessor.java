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
package org.smallmind.seda;

import java.util.concurrent.CountDownLatch;
import org.smallmind.scribe.pen.LoggerManager;

public class EventProcessor<I extends Event, O extends Event> implements Runnable {

   private CountDownLatch exitLatch;
   private SedaConfiguration sedaConfiguration;
   private EventQueue<I> eventQueue;
   private WorkMonitor monitor;
   private boolean stopped = false;

   public EventProcessor (EventQueue<I> eventQueue, DurationMonitor durationMonitor, SedaConfiguration sedaConfiguration) {

      this.eventQueue = eventQueue;

      monitor = new WorkMonitor(durationMonitor, sedaConfiguration.getWorkTrackingTime(), sedaConfiguration.getWorkTrackingTimeUnit());
      exitLatch = new CountDownLatch(1);
   }

   protected WorkMonitor getMonitor () {

      return monitor;
   }

   public double getIdlePercentage () {

      return monitor.getIdlePercentage();
   }

   public double getActivePercentage () {

      return monitor.getActivePercentage();
   }

   public boolean isRunning () {

      return !stopped;
   }

   protected void stop ()
      throws InterruptedException {

      stopped = true;
      exitLatch.await();
   }

   public void run () {

      I inputEvent;
      StopWatch stopWatch = new StopWatch();

      try {
         stopWatch.click();
         while (!stopped) {
            if ((inputEvent = eventQueue.poll(sedaConfiguration.getQueuePollTimeout(), sedaConfiguration.getQueuePollTimeUnit())) != null) {
               monitor.addIdleTime(stopWatch.click());
               //TODO: HandleEvent
               monitor.addActiveTime(stopWatch.click());
            }
            else {
               monitor.addIdleTime(stopWatch.click());
            }
         }
      }
      catch (InterruptedException interruptedException) {
         stopped = true;
         LoggerManager.getLogger(EventProcessor.class).error(interruptedException);
      }

      exitLatch.countDown();
   }
}

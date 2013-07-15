/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with the SmallMind Code Project. If not, see
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

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class HomeostaticRegulator<I extends Event, O extends Event> implements Runnable {

  private final LinkedList<EventProcessor<I, O>> processorList;

  private CountDownLatch exitLatch;
  private CountDownLatch pulseLatch;
  private AtomicBoolean stopped = new AtomicBoolean(false);
  private SedaConfiguration sedaConfiguration;
  private ThreadPool<I, O> threadPool;

  public HomeostaticRegulator (ThreadPool<I, O> threadPool, DurationMonitor durationMonitor, LinkedList<EventProcessor<I, O>> processorList, SedaConfiguration sedaConfiguration) {

    this.threadPool = threadPool;
    this.processorList = processorList;
    this.sedaConfiguration = sedaConfiguration;

    exitLatch = new CountDownLatch(1);
    pulseLatch = new CountDownLatch(1);
  }

  public boolean isRunning () {

    return !stopped.get();
  }

  protected void stop ()
    throws InterruptedException {

    if (stopped.compareAndSet(false, true)) {
      pulseLatch.countDown();
    }

    exitLatch.await();
  }

  public void run () {

    try {
      while (!stopped.get()) {
        try {
          pulseLatch.await(sedaConfiguration.getRegulatorPulseTime(), sedaConfiguration.getRegulatorPulseTimeUnit());
        }
        catch (InterruptedException interruptedException) {
          LoggerManager.getLogger(HomeostaticRegulator.class).error(interruptedException);
        }

        if (!stopped.get()) {

          double idlePercentage = 0;
          double activePercentage = 0;

          synchronized (processorList) {
            for (EventProcessor<I, O> eventProcessor : processorList) {
              idlePercentage += eventProcessor.getIdlePercentage();
              activePercentage += eventProcessor.getActivePercentage();
            }

            idlePercentage /= processorList.size();
            activePercentage /= processorList.size();
          }
          if (sedaConfiguration.getActiveUpShiftPercentage() >= activePercentage) {
            threadPool.increase();
          }
          else if (sedaConfiguration.getInactiveDownShiftPercentage() >= idlePercentage) {
            threadPool.decrease();
          }
        }
      }
    }
    finally {
      exitLatch.countDown();
    }
  }
}

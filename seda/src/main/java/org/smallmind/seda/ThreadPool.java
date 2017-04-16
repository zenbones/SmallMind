/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under either, at your discretion...
 * 
 * 1) The terms of GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 * 
 * ...or...
 * 
 * 2) The terms of the Apache License, Version 2.0.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or Apache License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the Apache License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://www.apache.org/licenses/LICENSE-2.0>.
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

public class ThreadPool<I extends Event, O extends Event> {

  private final LinkedList<EventProcessor<I, O>> processorList;

  private CountDownLatch exitLatch;
  private AtomicBoolean stopped = new AtomicBoolean(false);
  private SedaConfiguration sedaConfiguration;
  private EventQueue<I> eventQueue;
  private DurationMonitor durationMonitor;
  private HomeostaticRegulator<I, O> homeostaticRegulator;

  public ThreadPool (EventQueue<I> eventQueue, SedaConfiguration sedaConfiguration) {

    Thread regulatorThread;

    this.sedaConfiguration = sedaConfiguration;
    this.eventQueue = eventQueue;

    durationMonitor = new DurationMonitor(sedaConfiguration.getMaxTrackedInvocations());
    processorList = new LinkedList<EventProcessor<I, O>>();

    regulatorThread = new Thread(homeostaticRegulator = new HomeostaticRegulator<I, O>(this, durationMonitor, processorList, sedaConfiguration));
    regulatorThread.start();

    exitLatch = new CountDownLatch(1);
  }

  public boolean isRunning () {

    return !stopped.get();
  }

  protected synchronized void increase () {

    synchronized (processorList) {
      if (!stopped.get()) {
        if ((sedaConfiguration.getMaxThreadPoolSize() == 0) || (processorList.size() < sedaConfiguration.getMaxThreadPoolSize())) {

          Thread processorThread;
          EventProcessor<I, O> eventProcessor;

          eventProcessor = new EventProcessor<I, O>(eventQueue, durationMonitor, sedaConfiguration);
          processorThread = new Thread(eventProcessor);
          processorThread.start();

          processorList.add(eventProcessor);
        }
      }
    }
  }

  protected void decrease () {

    synchronized (processorList) {
      if (!stopped.get()) {
        if ((!processorList.isEmpty()) && (processorList.size() > sedaConfiguration.getMinThreadPoolSize())) {
          try {
            processorList.removeFirst().stop();
            // TODO: When the processor list is Empty (or 1), the processor should short circuit and decide how to upshift out of this state
          }
          catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
          }
        }
      }
    }
  }

  protected void stop ()
    throws InterruptedException {

    if (stopped.compareAndSet(false, true)) {
      try {
        homeostaticRegulator.stop();
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
      }

      synchronized (processorList) {
        while (!processorList.isEmpty()) {
          try {
            processorList.removeFirst().stop();
          }
          catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(ThreadPool.class).error(interruptedException);
          }
        }
      }

      exitLatch.countDown();
    }
    else {
      exitLatch.await();
    }
  }
}

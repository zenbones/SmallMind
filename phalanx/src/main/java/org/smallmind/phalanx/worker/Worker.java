/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.phalanx.worker;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class Worker<T> implements Runnable {

  private static final long MINIMUM_REPORTED_IDLE_TIME = 10;

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final WorkQueue<T> workQueue;
  private Thread runnableThread;

  public Worker (WorkQueue<T> workQueue) {

    this.workQueue = workQueue;
  }

  public abstract void engageWork (T transfer)
    throws Throwable;

  public abstract void close ()
    throws Exception;

  public void stop ()
    throws Exception {

    if (stopped.compareAndSet(false, true)) {
      close();

      if (runnableThread != null) {
        runnableThread.interrupt();
      }
    }
    exitLatch.await();
  }

  @Override
  public void run () {

    long idleStart = System.nanoTime();

    try {
      runnableThread = Thread.currentThread();

      while (!stopped.get()) {
        try {

          final T transfer;

          if ((transfer = workQueue.poll(1, TimeUnit.SECONDS)) != null) {

            long idleTime;

            if ((idleTime = System.nanoTime() - idleStart) >= MINIMUM_REPORTED_IDLE_TIME) {
              Instrument.with(Worker.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.WORKER_IDLE.getDisplay())).update(idleTime, TimeUnit.NANOSECONDS);
            }

            engageWork(transfer);
          }
        } catch (InterruptedException interruptedException) {
          if (!stopped.get()) {
            LoggerManager.getLogger(this.getClass()).error(interruptedException);
          }
        } catch (Throwable throwable) {
          LoggerManager.getLogger(this.getClass()).error(throwable);
        } finally {
          idleStart = System.nanoTime();
        }
      }
    } finally {
      exitLatch.countDown();
    }
  }
}

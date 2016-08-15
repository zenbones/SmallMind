/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.instrument.Clocks;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.instrument.config.MetricConfigurationProvider;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class Worker<T> implements Runnable, MetricConfigurationProvider {

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final MetricConfiguration metricConfiguration;
  private final TransferQueue<T> workTransferQueue;

  public Worker (MetricConfiguration metricConfiguration, TransferQueue<T> workTransferQueue) {

    this.metricConfiguration = metricConfiguration;
    this.workTransferQueue = workTransferQueue;
  }

  public abstract void engageWork (T transfer)
    throws Exception;

  public abstract void close ()
    throws Exception;

  @Override
  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  public void stop ()
    throws Exception {

    if (stopped.compareAndSet(false, true)) {
      close();
    }
    exitLatch.await();
  }

  @Override
  public void run () {

    long idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();

    try {
      while (!stopped.get()) {
        try {

          T transfer;

          if ((transfer = workTransferQueue.poll(1, TimeUnit.SECONDS)) != null) {
            InstrumentationManager.instrumentWithChronometer(this, Clocks.EPOCH.getClock().getTimeNanoseconds() - idleStart, TimeUnit.NANOSECONDS, new MetricProperty("event", MetricType.WORKER_IDLE.getDisplay()));

            engageWork(transfer);
          }
        } catch (Exception exception) {
          LoggerManager.getLogger(this.getClass()).error(exception);
        } finally {
          idleStart = Clocks.EPOCH.getClock().getTimeNanoseconds();
        }
      }
    } finally {
      exitLatch.countDown();
    }
  }
}

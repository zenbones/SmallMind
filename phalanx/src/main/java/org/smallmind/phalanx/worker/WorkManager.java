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

import java.lang.reflect.Array;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TransferQueue;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.scribe.pen.LoggerManager;

public class WorkManager<W extends Worker<T>, T> {

  private final TransferQueue<T> transferQueue;
  private final Class<W> workerClass;
  private final int concurrencyLimit;
  private W[] workers;

  public WorkManager (Class<W> workerClass, int concurrencyLimit) {

    if (concurrencyLimit <= 0) {
      throw new IllegalArgumentException("The concurrency limit must be > 0");
    }

    this.workerClass = workerClass;
    this.concurrencyLimit = concurrencyLimit;

    transferQueue = new LinkedTransferQueue<>();
  }

  public int getConcurrencyLimit () {

    return concurrencyLimit;
  }

  public void startUp (MetricConfiguration metricConfiguration, WorkerFactory<W, T> workerFactory) {

    workers = (W[])Array.newInstance(workerClass, concurrencyLimit);
    for (int index = 0; index < workers.length; index++) {

      Thread workerThread = new Thread(workers[index] = workerFactory.createWorker(metricConfiguration, transferQueue));

      workerThread.setDaemon(true);
      workerThread.start();
    }
  }

  public boolean execute (final T transfer)
    throws InterruptedException {

    return transferQueue.tryTransfer(transfer, 1, TimeUnit.SECONDS);
  }

  public void shutDown () {

    if (workers != null) {
      for (W worker : workers) {
        try {
          worker.stop();
        } catch (Exception exception) {
          LoggerManager.getLogger(WorkManager.class).error(exception);
        }
      }
    }
  }
}


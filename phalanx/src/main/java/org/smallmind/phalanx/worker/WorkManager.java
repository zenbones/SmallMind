/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.scribe.pen.LoggerManager;

public class WorkManager<W extends Worker<T>, T> {

  private static enum State {STOPPED, STARTING, STARTED, STOPPING}

  private final AtomicReference<State> stateRef = new AtomicReference<>(State.STOPPED);
  private final MetricConfiguration metricConfiguration;
  private final WorkQueue<T> workQueue;
  private final Class<W> workerClass;
  private final int concurrencyLimit;
  private W[] workers;

  public WorkManager (MetricConfiguration metricConfiguration, Class<W> workerClass, int concurrencyLimit) {

    this(metricConfiguration, workerClass, concurrencyLimit, new TransferringWorkQueue<>());
  }

  public WorkManager (MetricConfiguration metricConfiguration, Class<W> workerClass, int concurrencyLimit, WorkQueue<T> workQueue) {

    this.metricConfiguration = metricConfiguration;
    this.workerClass = workerClass;
    this.concurrencyLimit = concurrencyLimit;
    this.workQueue = workQueue;
  }

  public MetricConfiguration getMetricConfiguration () {

    return metricConfiguration;
  }

  public int getConcurrencyLimit () {

    return concurrencyLimit;
  }

  public void startUp (WorkerFactory<W, T> workerFactory)
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STOPPED, State.STARTING)) {

      workers = (W[])Array.newInstance(workerClass, concurrencyLimit);
      for (int index = 0; index < workers.length; index++) {

        Thread workerThread = new Thread(workers[index] = workerFactory.createWorker(metricConfiguration, workQueue));

        workerThread.setDaemon(true);
        workerThread.start();
      }

      stateRef.set(State.STARTED);
    } else {
      while (State.STARTING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  public void execute (final T work)
    throws Exception {

    if (!State.STARTED.equals(stateRef.get())) {
      throw new WorkManagerException("%s is not in the 'started' state", WorkManager.class.getSimpleName());
    }

    InstrumentationManager.execute(new ChronometerInstrument(metricConfiguration, new MetricProperty("event", MetricInteraction.ACQUIRE_WORKER.getDisplay())) {

      @Override
      public void withChronometer ()
        throws InterruptedException {

        boolean success;

        do {
          success = workQueue.offer(work, 1, TimeUnit.SECONDS);
        } while (!success);
      }
    });
  }

  public void shutDown ()
    throws InterruptedException {

    if (stateRef.compareAndSet(State.STARTED, State.STOPPING)) {
      for (W worker : workers) {
        try {
          worker.stop();
        } catch (Exception exception) {
          LoggerManager.getLogger(WorkManager.class).error(exception);
        }
      }
      stateRef.set(State.STOPPED);
    } else {
      while (State.STOPPING.equals(stateRef.get())) {
        Thread.sleep(100);
      }
    }
  }
}


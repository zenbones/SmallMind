/*
 * Copyright (c) 2007 through 2026 David Berkman
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
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Coordinates a set of worker instances, handling lifecycle and queuing of work items.
 *
 * @param <W> concrete worker type that will process items
 * @param <T> type of work accepted by the queue
 */
public class WorkManager<W extends Worker<T>, T> {

  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final WorkQueue<T> workQueue;
  private final Class<W> workerClass;
  private final int concurrencyLimit;
  private W[] workers;

  /**
   * Creates a manager with a default transferring work queue.
   *
   * @param workerClass      class of the worker to instantiate
   * @param concurrencyLimit maximum number of concurrently running workers
   */
  public WorkManager (Class<W> workerClass, int concurrencyLimit) {

    this(workerClass, concurrencyLimit, new TransferringWorkQueue<>());
  }

  /**
   * Creates a manager with the supplied queue implementation.
   *
   * @param workerClass      class of the worker to instantiate
   * @param concurrencyLimit maximum number of concurrently running workers
   * @param workQueue        queue used to hand off work items to workers
   */
  public WorkManager (Class<W> workerClass, int concurrencyLimit, WorkQueue<T> workQueue) {

    this.workerClass = workerClass;
    this.concurrencyLimit = concurrencyLimit;
    this.workQueue = workQueue;
  }

  /**
   * Returns the configured concurrency cap for this manager.
   *
   * @return maximum worker count
   */
  public int getConcurrencyLimit () {

    return concurrencyLimit;
  }

  /**
   * Starts the worker pool if not already running.
   *
   * @param workerFactory factory used to construct worker instances
   * @throws InterruptedException if the thread is interrupted while waiting on startup
   */
  public void startUp (WorkerFactory<W, T> workerFactory)
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {

      workers = (W[])Array.newInstance(workerClass, concurrencyLimit);
      for (int index = 0; index < workers.length; index++) {

        Thread workerThread = new Thread(workers[index] = workerFactory.createWorker(workQueue));

        workerThread.setDaemon(true);
        workerThread.start();
      }

      statusRef.set(ComponentStatus.STARTED);
    } else {
      while (ComponentStatus.STARTING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  /**
   * Enqueues work for processing, blocking until a worker accepts it.
   *
   * @param work the item to process
   * @throws Throwable if the manager is not started or enqueueing fails
   */
  public void execute (final T work)
    throws Throwable {

    if (!ComponentStatus.STARTED.equals(statusRef.get())) {
      throw new WorkManagerException("%s is not in the 'started' state", WorkManager.class.getSimpleName());
    }

    Instrument.with(WorkManager.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_WORKER.getDisplay())).on(() -> {

      boolean success;

      do {
        success = workQueue.offer(work, 1, TimeUnit.SECONDS);
      } while (!success);
    });
  }

  /**
   * Stops all workers and transitions the manager to the stopped state.
   *
   * @throws InterruptedException if interrupted while waiting for stop completion
   */
  public void shutDown ()
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      for (W worker : workers) {
        try {
          worker.stop();
        } catch (Exception exception) {
          LoggerManager.getLogger(WorkManager.class).error(exception);
        }
      }
      statusRef.set(ComponentStatus.STOPPED);
    } else {
      while (ComponentStatus.STOPPING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }
}

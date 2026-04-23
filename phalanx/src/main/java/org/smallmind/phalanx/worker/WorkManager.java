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
 * Manages a fixed-size pool of {@link Worker} instances sharing a common {@link WorkQueue}.
 *
 * <p>The manager owns the full lifecycle of its workers: it creates and starts them via a
 * {@link WorkerFactory} on {@link #startUp}, dispatches work items through {@link #execute},
 * and cooperatively shuts them down via {@link #shutDown}.  Metrics for work-acquisition latency
 * are recorded with Claxon.  Concurrent calls to {@code startUp} or {@code shutDown} are safe;
 * subsequent callers spin-wait until the transition in progress completes.</p>
 *
 * @param <W> the concrete {@link Worker} subtype managed by this instance
 * @param <T> the type of work items accepted by the underlying queue
 */
public class WorkManager<W extends Worker<T>, T> {

  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final WorkQueue<T> workQueue;
  private final Class<W> workerClass;
  private final int concurrencyLimit;
  private W[] workers;

  /**
   * Creates a manager that uses a {@link TransferringWorkQueue} as the default queue implementation.
   *
   * @param workerClass      the runtime class of the worker type, used to allocate the worker array
   * @param concurrencyLimit the maximum number of worker threads to run concurrently
   */
  public WorkManager (Class<W> workerClass, int concurrencyLimit) {

    this(workerClass, concurrencyLimit, new TransferringWorkQueue<>());
  }

  /**
   * Creates a manager with an explicitly supplied queue implementation.
   *
   * @param workerClass      the runtime class of the worker type, used to allocate the worker array
   * @param concurrencyLimit the maximum number of worker threads to run concurrently
   * @param workQueue        the queue through which work items are handed off to workers
   */
  public WorkManager (Class<W> workerClass, int concurrencyLimit, WorkQueue<T> workQueue) {

    this.workerClass = workerClass;
    this.concurrencyLimit = concurrencyLimit;
    this.workQueue = workQueue;
  }

  /**
   * Returns the maximum number of workers this manager will run concurrently.
   *
   * @return the concurrency limit supplied at construction time
   */
  public int getConcurrencyLimit () {

    return concurrencyLimit;
  }

  /**
   * Starts the worker pool if it is not already started.
   *
   * <p>Workers are created via {@code workerFactory}, wrapped in daemon threads, and started immediately.
   * If a start is already in progress the calling thread spin-waits until it completes.</p>
   *
   * @param workerFactory factory used to create each worker instance
   * @throws InterruptedException if the calling thread is interrupted while spin-waiting for a concurrent start
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
   * Submits a work item for processing, blocking with retries until the queue accepts it.
   *
   * <p>The method measures and records work-acquisition time via Claxon.  The manager must be in the
   * {@code STARTED} state; otherwise a {@link WorkManagerException} is thrown.</p>
   *
   * @param work the work item to enqueue
   * @throws WorkManagerException if the manager is not in the {@code STARTED} state
   * @throws Throwable            if the Claxon instrumentation or queue interaction throws unexpectedly
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
   * Stops all workers and transitions the manager to the {@code STOPPED} state.
   *
   * <p>Each worker's {@link Worker#stop} method is called in sequence; any exception thrown is logged
   * and the remaining workers are still stopped.  If a shutdown is already in progress the calling
   * thread spin-waits until it completes.</p>
   *
   * @throws InterruptedException if the calling thread is interrupted while spin-waiting for a concurrent shutdown
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

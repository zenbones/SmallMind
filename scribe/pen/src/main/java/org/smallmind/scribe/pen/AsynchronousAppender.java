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
package org.smallmind.scribe.pen;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Appender decorator that accepts log records on the calling thread, buffers them in a
 * {@link LinkedBlockingQueue}, and forwards them to a wrapped appender on one or more dedicated
 * daemon worker threads, decoupling log callers from the cost of actual I/O.
 */
public class AsynchronousAppender extends AbstractWrappedAppender {

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final LinkedBlockingQueue<Record<?>> publishQueue;
  private final PublishWorker[] publishWorkers;
  private final int bufferSize;

  /**
   * Constructs an asynchronous appender with a single-record buffer and one worker thread.
   *
   * @param internalAppender the underlying appender that records are ultimately forwarded to
   */
  public AsynchronousAppender (Appender internalAppender) {

    this(internalAppender, 1, 1);
  }

  /**
   * Constructs an asynchronous appender with a configurable queue capacity and worker-thread count.
   * Each worker thread is started immediately as a daemon thread and begins draining the queue.
   * Values below 1 for either numeric argument are clamped to 1.
   *
   * @param internalAppender the underlying appender that records are ultimately forwarded to
   * @param bufferSize       maximum number of records that may be queued before new records are rejected
   * @param concurrencyLimit number of parallel worker threads draining the queue
   */
  public AsynchronousAppender (Appender internalAppender, int bufferSize, int concurrencyLimit) {

    super(internalAppender);

    this.bufferSize = Math.max(1, bufferSize);

    publishQueue = new LinkedBlockingQueue<>(this.bufferSize);
    publishWorkers = new PublishWorker[Math.max(1, concurrencyLimit)];

    for (int index = 0; index < publishWorkers.length; index++) {

      Thread publishThread;

      publishThread = new Thread(publishWorkers[index] = new PublishWorker());
      publishThread.setDaemon(true);
      publishThread.start();
    }
  }

  /**
   * Enqueues the given record for asynchronous delivery to the wrapped appender. If this appender
   * has already been closed, or if the queue is at capacity, a {@link LoggerException} is created
   * and routed to the configured error handler rather than thrown to the caller.
   *
   * @param record the log record to enqueue for asynchronous publication
   */
  @Override
  public void publish (Record<?> record) {

    try {
      if (finished.get()) {
        throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
      } else if (!publishQueue.offer(record)) {
        throw new LoggerException("Buffer exceeded(%d) on %s", bufferSize, AsynchronousAppender.class.getSimpleName());
      }
    } catch (Exception exception) {
      handleError(record, exception);
    }
  }

  /**
   * Signals all worker threads to stop processing, waits for each to exit via its
   * {@link CountDownLatch}, and then closes the wrapped appender.
   *
   * @throws InterruptedException if the current thread is interrupted while waiting for a worker to finish
   * @throws LoggerException      if the wrapped appender throws during its own {@code close()}
   */
  @Override
  public void close ()
    throws InterruptedException, LoggerException {

    for (PublishWorker publishWorker : publishWorkers) {
      publishWorker.finish();
    }

    super.close();
  }

  private class PublishWorker implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private Thread runnableThread;

    /**
     * Signals this worker to stop accepting new records and waits until its run loop has exited.
     * If the finished flag has not already been set, the worker thread is interrupted to break
     * out of any blocking queue poll.
     *
     * @throws InterruptedException if the current thread is interrupted while awaiting the exit latch
     */
    private void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        runnableThread.interrupt();
      }
      exitLatch.await();
    }

    /**
     * Runs the worker loop, polling the queue in one-second intervals and forwarding each dequeued
     * record to the wrapped appender. Any exception during forwarding is routed to the configured
     * error handler. The loop exits when the finished flag is set or the thread is interrupted, after
     * which the exit latch is counted down to unblock any caller waiting in {@link #finish()}.
     */
    public void run () {

      try {
        runnableThread = Thread.currentThread();

        while (!finished.get()) {

          Record<?> record = null;

          try {
            if ((record = publishQueue.poll(1, TimeUnit.SECONDS)) != null) {
              publishToWrappedAppender(record);
            }
          } catch (InterruptedException interruptedException) {
            finished.set(true);
          } catch (Exception exception) {
            if (record == null) {
              handleError(Logger.unknown(), exception);
            } else {
              handleError(record, exception);
            }
          }
        }
      } finally {
        exitLatch.countDown();
      }
    }
  }
}

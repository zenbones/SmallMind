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
package org.smallmind.sleuth.runner;

import java.util.concurrent.Semaphore;

/**
 * Concurrency gate that limits the number of simultaneously executing controllers per {@link TestTier}.
 * <p>
 * One fair {@link Semaphore} is maintained for each tier. When a controller is submitted, the caller
 * blocks until a permit is available in the tier's semaphore. If the runner has been cancelled the
 * controller's {@link TestController#complete()} is called instead of starting a thread, so that
 * latches are correctly decremented without doing real work. The controller is responsible for calling
 * {@link #release(TestTier)} — typically in its {@code complete()} method — to return the permit.
 *
 * @see TestTier
 * @see TestController
 */
public class SleuthThreadPool {

  private final SleuthRunner sleuthRunner;
  private final Semaphore[] semaphores;

  /**
   * Constructs a thread pool with one semaphore per {@link TestTier}, each initialised with
   * {@code threadCount} permits.
   *
   * @param sleuthRunner runner consulted before starting each thread to detect cancellation;
   *                     must not be {@code null}
   * @param threadCount  number of concurrent slots per tier; must be positive
   */
  public SleuthThreadPool (SleuthRunner sleuthRunner, int threadCount) {

    this.sleuthRunner = sleuthRunner;

    semaphores = new Semaphore[TestTier.values().length];

    for (TestTier testTier : TestTier.values()) {
      semaphores[testTier.ordinal()] = new Semaphore(threadCount, true);
    }
  }

  /**
   * Acquires a permit for the given tier and either starts the controller on a new thread or
   * calls {@link TestController#complete()} if execution has been cancelled.
   * <p>
   * The caller blocks until a slot is available in the tier's semaphore. If the thread is
   * interrupted after the permit is acquired, the interrupt flag is restored and an
   * {@link InterruptedException} is thrown.
   *
   * @param testTier   tier whose semaphore governs this submission; must not be {@code null}
   * @param controller work to execute; must not be {@code null}
   * @throws InterruptedException if the calling thread is interrupted while waiting for a permit
   *                              or after the new thread is started and reports interruption
   */
  public void execute (TestTier testTier, TestController controller)
    throws InterruptedException {

    semaphores[testTier.ordinal()].acquire();

    if (sleuthRunner.isRunning()) {

      Thread thread = new Thread(controller);

      thread.start();
      if (thread.isInterrupted()) {
        throw new InterruptedException();
      }
    } else {
      controller.complete();
    }
  }

  /**
   * Returns one permit to the semaphore for the given tier, potentially unblocking a caller
   * waiting in {@link #execute}.
   *
   * @param testTier tier whose semaphore should be released; must not be {@code null}
   */
  public void release (TestTier testTier) {

    semaphores[testTier.ordinal()].release();
  }
}

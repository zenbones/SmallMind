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
 * Concurrency gate that limits parallel execution per {@link TestTier}.
 */
public class SleuthThreadPool {

  private final SleuthRunner sleuthRunner;
  private final Semaphore[] semaphores;

  /**
   * Creates semaphores for each test tier.
   *
   * @param sleuthRunner runner used to check cancellation
   * @param threadCount  permits allowed per tier
   */
  public SleuthThreadPool (SleuthRunner sleuthRunner, int threadCount) {

    this.sleuthRunner = sleuthRunner;

    semaphores = new Semaphore[TestTier.values().length];

    for (TestTier testTier : TestTier.values()) {
      semaphores[testTier.ordinal()] = new Semaphore(threadCount, true);
    }
  }

  /**
   * Attempts to execute a controller in its tier, blocking until a slot is available.
   *
   * @param testTier   tier controlling which semaphore to use
   * @param controller runnable controller to execute
   * @throws InterruptedException if interrupted while waiting for a permit
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
   * Releases a permit for the given tier.
   *
   * @param testTier tier to release
   */
  public void release (TestTier testTier) {

    semaphores[testTier.ordinal()].release();
  }
}

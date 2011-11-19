/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
 *
 * This file is part of the SmallMind Code Project.
 *
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.pool2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class UnreturnedConnectionTimeoutDeconstructionFuse<C> extends DeconstructionFuse<C> {

  private final ConnectionPool<C> connectionPool;
  private final CyclicBarrier freeBarrier;
  private final CyclicBarrier serveBarrier;
  private final CountDownLatch initLatch = new CountDownLatch(1);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final AtomicBoolean aborted = new AtomicBoolean(false);

  private Thread runnableThread;

  protected UnreturnedConnectionTimeoutDeconstructionFuse (ConnectionPool<C> connectionPool) {

    this.connectionPool = connectionPool;

    freeBarrier = new CyclicBarrier(2);
    serveBarrier = new CyclicBarrier(2);
  }

  public void free () {

    try {
      freeBarrier.await();
    }
    catch (Exception exception) {
      if (!aborted.get()) {
        LoggerManager.getLogger(UnreturnedConnectionTimeoutDeconstructionFuse.class).error(exception);
      }
    }
  }

  public void serve () {

    try {
      serveBarrier.await();
    }
    catch (Exception exception) {
      if (!aborted.get()) {
        LoggerManager.getLogger(UnreturnedConnectionTimeoutDeconstructionFuse.class).error(exception);
      }
    }
  }

  @Override
  public void abort () {

    if (aborted.compareAndSet(false, true)) {
      try {
        initLatch.await();
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(UnreturnedConnectionTimeoutDeconstructionFuse.class).error(interruptedException);
      }

      runnableThread.interrupt();

      try {
        exitLatch.await();
      }
      catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(UnreturnedConnectionTimeoutDeconstructionFuse.class).error(interruptedException);
      }
    }
  }

  public void run () {

    runnableThread = Thread.currentThread();
    initLatch.countDown();

    try {
      while (!aborted.get()) {
        serveBarrier.await();
        freeBarrier.await(connectionPool.getConnectionPoolConfig().getUnreturnedConnectionTimeoutSeconds(), TimeUnit.SECONDS);
      }
    }
    catch (TimeoutException timeoutException) {
      ignite();
    }
    catch (Exception exception) {
      if (!aborted.get()) {
        LoggerManager.getLogger(UnreturnedConnectionTimeoutDeconstructionFuse.class).error(exception);
      }
    }

    exitLatch.countDown();
  }
}
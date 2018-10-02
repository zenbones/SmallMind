/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.web.reverse;

import java.nio.channels.SocketChannel;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.scribe.pen.LoggerManager;

public class ProxyExecutor {

  private final CountDownLatch exitLatch;
  private final ExecutionWorker[] workers;
  private final int concurrencyLimit;

  public ProxyExecutor (int concurrencyLimit) {

    this.concurrencyLimit = concurrencyLimit;

    exitLatch = new CountDownLatch(concurrencyLimit);
    workers = new ExecutionWorker[concurrencyLimit];

    for (int index = 0; index < workers.length; index++) {
      new Thread(workers[index] = new ExecutionWorker()).start();
    }
  }

  public void execute (SocketChannel sourceChannel, Runnable runnable) {

    workers[sourceChannel.hashCode() % concurrencyLimit].execute(runnable);
  }

  public void shutdown () {

    for (ExecutionWorker worker : workers) {
      worker.abort();
    }

    try {
      exitLatch.await();
    } catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(ProxyExecutor.class).error(interruptedException);
    }
  }

  private class ExecutionWorker implements Runnable {

    private AtomicBoolean stopped = new AtomicBoolean(false);
    private LinkedBlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();

    private void abort () {

      stopped.compareAndSet(false, true);
    }

    private void execute (Runnable runnable) {

      runnableQueue.add(runnable);
    }

    @Override
    public void run () {

      try {
        while (!stopped.get()) {

          Runnable runnable;

          if ((runnable = runnableQueue.poll(1, TimeUnit.SECONDS)) != null) {
            runnable.run();
          }
        }
      } catch (InterruptedException interruptedException) {
        LoggerManager.getLogger(ProxyExecutor.class).error(interruptedException);
      } finally {
        exitLatch.countDown();
      }
    }
  }
}

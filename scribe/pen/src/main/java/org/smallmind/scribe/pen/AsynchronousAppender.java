/*
 * Copyright (c) 2007 through 2024 David Berkman
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

public class AsynchronousAppender extends AbstractWrappedAppender {

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final LinkedBlockingQueue<Record<?>> publishQueue;
  private final PublishWorker publishWorker;
  private final int bufferSize;

  public AsynchronousAppender (Appender internalAppender) {

    this(internalAppender, Integer.MAX_VALUE);
  }

  public AsynchronousAppender (Appender internalAppender, int bufferSize) {

    super(internalAppender);

    Thread publishThread;

    this.bufferSize = bufferSize;

    publishQueue = new LinkedBlockingQueue<>(bufferSize);

    publishThread = new Thread(publishWorker = new PublishWorker());
    publishThread.setDaemon(true);
    publishThread.start();
  }

  @Override
  public void publish (Record<?> record) {

    try {
      if (finished.get()) {
        throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
      }
      if (!publishQueue.offer(record)) {
        throw new LoggerException("Buffer exceeded(%d) on %s", bufferSize, AsynchronousAppender.class.getSimpleName());
      }
    } catch (Exception exception) {
      if (getErrorHandler() == null) {
        exception.printStackTrace();
      } else {
        getErrorHandler().process(record, exception, "Unable to publish message from appender(%s)", (getName() != null) ? getName() : this.getClass().getCanonicalName());
      }
    }
  }

  public void close ()
    throws InterruptedException, LoggerException {

    publishWorker.finish();

    super.close();
  }

  private class PublishWorker implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private Thread runnableThread;

    private void finish ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        runnableThread.interrupt();
      }
      exitLatch.await();
    }

    public void run () {

      try {
        runnableThread = Thread.currentThread();

        while (!finished.get()) {
          try {

            Record<?> record;

            if ((record = publishQueue.poll(1, TimeUnit.SECONDS)) != null) {
              publishToWrappedAppender(record);
            }
          } catch (InterruptedException interruptedException) {
            finished.set(true);
          } catch (Exception exception) {
            LoggerManager.getLogger(AsynchronousAppender.class).error(exception);
          }
        }
      } finally {
        exitLatch.countDown();
      }
    }
  }
}

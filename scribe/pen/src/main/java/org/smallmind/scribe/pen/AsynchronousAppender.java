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
package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsynchronousAppender implements Appender {

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final Appender internalAppender;
  private final LinkedBlockingQueue<Record> publishQueue;
  private final PublishWorker publishWorker;

  public AsynchronousAppender (Appender internalAppender) {

    this(internalAppender, Integer.MAX_VALUE);
  }

  public AsynchronousAppender (Appender internalAppender, int bufferSize) {

    Thread publishThread;

    this.internalAppender = internalAppender;

    publishQueue = new LinkedBlockingQueue<>(bufferSize);

    publishThread = new Thread(publishWorker = new PublishWorker());
    publishThread.setDaemon(true);
    publishThread.start();
  }

  public String getName () {

    return internalAppender.getName();
  }

  public void setName (String name) {

    internalAppender.setName(name);
  }

  public void clearFilters () {

    internalAppender.clearFilters();
  }

  public synchronized void setFilter (Filter filter) {

    internalAppender.setFilter(filter);
  }

  public void addFilter (Filter filter) {

    internalAppender.addFilter(filter);
  }

  public Filter[] getFilters () {

    return internalAppender.getFilters();
  }

  public void setFilters (List<Filter> filterList) {

    internalAppender.setFilters(filterList);
  }

  public ErrorHandler getErrorHandler () {

    return internalAppender.getErrorHandler();
  }

  public void setErrorHandler (ErrorHandler errorHandler) {

    internalAppender.setErrorHandler(errorHandler);
  }

  public Formatter getFormatter () {

    return internalAppender.getFormatter();
  }

  public void setFormatter (Formatter formatter) {

    internalAppender.setFormatter(formatter);
  }

  public boolean isActive () {

    return internalAppender.isActive();
  }

  public void setActive (boolean active) {

    internalAppender.setActive(active);
  }

  public void publish (Record record) {

    try {
      if (finished.get()) {
        throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
      }

      publishQueue.put(record);
    } catch (InterruptedException interruptedException) {
      // nothing to do here
    } catch (Exception exception) {
      if (internalAppender.getErrorHandler() == null) {
        exception.printStackTrace();
      } else {
        internalAppender.getErrorHandler().process(record, exception, "Unable to publish message from appender(%s)", this.getClass().getCanonicalName());
      }
    }
  }

  public void close ()
    throws InterruptedException, LoggerException {

    publishWorker.finish();
    internalAppender.close();
  }

  protected void finalize ()
    throws InterruptedException, LoggerException {

    close();
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

            Record record;

            if ((record = publishQueue.poll(1, TimeUnit.SECONDS)) != null) {
              internalAppender.publish(record);
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
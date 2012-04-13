/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
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
package org.smallmind.scribe.pen;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class AsynchronousAppender implements Appender, Runnable {

  private CountDownLatch exitLatch;
  private Appender internalAppender;
  private LinkedBlockingQueue<Record> publishQueue;
  private boolean finished = false;

  public AsynchronousAppender (Appender internalAppender) {

    Thread publishThread;

    this.internalAppender = internalAppender;

    publishQueue = new LinkedBlockingQueue<Record>();

    exitLatch = new CountDownLatch(1);

    publishThread = new Thread(this);
    publishThread.setDaemon(true);
    publishThread.start();
  }

  public void setName (String name) {

    internalAppender.setName(name);
  }

  public String getName () {

    return internalAppender.getName();
  }

  public void clearFilters () {

    internalAppender.clearFilters();
  }

  public synchronized void setFilter (Filter filter) {

    internalAppender.setFilter(filter);
  }

  public void setFilters (List<Filter> filterList) {

    internalAppender.setFilters(filterList);
  }

  public void addFilter (Filter filter) {

    internalAppender.addFilter(filter);
  }

  public Filter[] getFilters () {

    return internalAppender.getFilters();
  }

  public ErrorHandler getErrorHandler () {

    return internalAppender.getErrorHandler();
  }

  public void setErrorHandler (ErrorHandler errorHandler) {

    internalAppender.setErrorHandler(errorHandler);
  }

  public void setFormatter (Formatter formatter) {

    internalAppender.setFormatter(formatter);
  }

  public Formatter getFormatter () {

    return internalAppender.getFormatter();
  }

  public boolean requiresFormatter () {

    return internalAppender.requiresFormatter();
  }

  public void publish (Record record) {

    try {
      if (finished) {
        throw new LoggerException("%s has been previously closed", this.getClass().getSimpleName());
      }

      publishQueue.put(record);
    }
    catch (Exception exception) {
      if (internalAppender.getErrorHandler() == null) {
        exception.printStackTrace();
      }
      else {
        internalAppender.getErrorHandler().process(record, exception, "Fatal error in appender(%s)", this.getClass().getCanonicalName());
      }
    }
  }

  public void close ()
    throws LoggerException {

    try {
      finish();
    }
    catch (InterruptedException interuptedException) {
      throw new LoggerException(interuptedException);
    }

    internalAppender.close();
  }

  public void finish ()
    throws InterruptedException {

    finished = true;
    exitLatch.await();
  }

  protected void finalize ()
    throws InterruptedException {

    finish();
  }

  public void run () {

    Record record;

    try {
      while (!(finished && publishQueue.isEmpty())) {
        if ((record = publishQueue.poll(1000, TimeUnit.MILLISECONDS)) != null) {
          internalAppender.publish(record);
        }
      }
    }
    catch (InterruptedException interruptedException) {
      finished = true;
      LoggerManager.getLogger(AsynchronousAppender.class).error(interruptedException);
    }
    finally {
      exitLatch.countDown();
    }
  }
}
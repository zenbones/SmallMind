/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
package org.smallmind.schedule.quartz;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.smallmind.nutsnbolts.util.SuccessOrFailure;
import org.smallmind.schedule.base.ProxyJob;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class QuartzProxyJob implements ProxyJob, InterruptableJob {

  private AtomicReference<Thread> threadRef = new AtomicReference<>();
  private AtomicReference<SuccessOrFailure> statusRef = new AtomicReference<>(SuccessOrFailure.SUCCESS);
  private LinkedList<Throwable> throwableList;

  private Date startTime;
  private Date stopTime;
  private int count = 0;

  public QuartzProxyJob () {

    throwableList = new LinkedList<>();
  }

  @Override
  public boolean isEnabled () {

    return true;
  }

  @Override
  public SuccessOrFailure getJobStatus () {

    return statusRef.get();
  }

  @Override
  public Date getStartTime () {

    return startTime;
  }

  @Override
  public Date getStopTime () {

    return stopTime;
  }

  @Override
  public synchronized void incCount () {

    count++;
  }

  public synchronized void addToCount (int additional) {

    count += additional;
  }

  @Override
  public synchronized int getCount () {

    return count;
  }

  @Override
  public synchronized Throwable[] getThrowables () {

    if (!throwableList.isEmpty()) {

      Throwable[] throwables;

      throwables = new Exception[throwableList.size()];
      throwableList.toArray(throwables);

      return throwables;
    }

    return null;
  }

  @Override
  public synchronized void setThrowable (Throwable throwable) {

    setThrowable(throwable, true);
  }

  public synchronized void setThrowable (Throwable throwable, boolean isFailure) {

    throwableList.add(throwable);

    if (isFailure) {
      statusRef.set(SuccessOrFailure.FAILURE);
    }

    LoggerManager.getLogger(this.getClass()).error(throwable);
  }

  @Override
  public void interrupt () {

    Thread thread;

    if ((thread = threadRef.get()) != null) {
      thread.interrupt();
    }
  }

  @Override
  public void execute (JobExecutionContext jobExecutionContext) {

    if (isEnabled()) {
      startTime = new Date();

      try {
        threadRef.set(Thread.currentThread());
        proceed();
      } catch (InterruptedException interruptedException) {
        statusRef.set(SuccessOrFailure.INTERRUPTED);
      } catch (Exception exception) {
        setThrowable(exception);
      } finally {

        SuccessOrFailure status;

        threadRef.set(null);
        stopTime = new Date();
        status = statusRef.get();

        if (SuccessOrFailure.INTERRUPTED.equals(status)) {
          LoggerManager.getLogger(this.getClass()).info("Job(%s) start(%s) has been interrupted", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
        } else if (SuccessOrFailure.FAILURE.equals(status) || (count > 0) || logOnZeroCount()) {
          LoggerManager.getLogger(this.getClass()).info("Job(%s) start(%s) stop(%s) count(%d) state(%s)", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
        }

        try {
          cleanup();
        } catch (Exception exception) {
          LoggerManager.getLogger(this.getClass()).error(exception);
        }
      }
    }
  }
}
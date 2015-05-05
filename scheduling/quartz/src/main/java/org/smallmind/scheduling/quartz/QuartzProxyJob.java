/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
package org.smallmind.scheduling.quartz;

import java.util.Date;
import java.util.LinkedList;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.smallmind.scheduling.base.JobStatus;
import org.smallmind.scheduling.base.ProxyJob;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class QuartzProxyJob implements ProxyJob, Job {

  private LinkedList<Throwable> throwableList;
  private JobStatus status = JobStatus.SUCCESS;
  private Date startTime;
  private Date stopTime;
  private int count = 0;

  public QuartzProxyJob () {

    throwableList = new LinkedList<>();
  }

  @Override
  public JobStatus getJobStatus () {

    return status;
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
      status = JobStatus.FAILURE;
    }

    LoggerManager.getLogger(this.getClass()).error(throwable);
  }

  @Override
  public void execute (JobExecutionContext jobExecutionContext)
    throws JobExecutionException {

    startTime = new Date();
    try {
      proceed();
    }
    catch (Exception exception) {
      setThrowable(exception);
    }
    finally {
      stopTime = new Date();

      if (status.equals(JobStatus.FAILURE) || (count > 0) || logOnZeroCount()) {
        LoggerManager.getLogger(this.getClass()).info("Job(%s) start(%s) stop(%s) count(%d) state(%s)", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
      }

      try {
        cleanup();
      }
      catch (Exception exception) {
        LoggerManager.getLogger(this.getClass()).error(exception);
      }
    }
  }
}
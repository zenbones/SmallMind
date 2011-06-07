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
package org.smallmind.scheduling.quartz;

import java.util.Date;
import java.util.LinkedList;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.StatefulJob;
import org.smallmind.scheduling.base.JobStatus;
import org.smallmind.scheduling.base.ProxyJob;
import org.smallmind.scribe.pen.LoggerManager;

public abstract class QuartzProxyJob implements ProxyJob, StatefulJob {

  private LinkedList<Exception> exceptionList;
  private JobStatus status = JobStatus.SUCCESS;
  private Date startTime;
  private Date stopTime;
  private int count = 0;

  public QuartzProxyJob () {

    exceptionList = new LinkedList<Exception>();
  }

  public JobStatus getJobStatus () {

    return status;
  }

  public Date getStartTime () {

    return startTime;
  }

  public Date getStopTime () {

    return stopTime;
  }

  public synchronized void incCount () {

    count++;
  }

  public synchronized void addToCount (int additional) {

    count += additional;
  }

  public synchronized int getCount () {

    return count;
  }

  public synchronized Exception[] getExceptions () {

    if (!exceptionList.isEmpty()) {

      Exception[] exceptions;

      exceptions = new Exception[exceptionList.size()];
      exceptionList.toArray(exceptions);

      return exceptions;
    }

    return null;
  }

  public synchronized void setException (Exception exception) {

    setException(exception, true);
  }

  public synchronized void setException (Exception exception, boolean isFailure) {

    exceptionList.add(exception);
    LoggerManager.getLogger(this.getClass()).error(exception);

    if (isFailure) {
      status = JobStatus.FAILURE;
    }
  }

  public void execute (JobExecutionContext jobExecutionContext)
    throws JobExecutionException {

    startTime = new Date();
    try {
      proceed();
      shutdown();
    }
    catch (Exception exception) {
      setException(exception);
      throw new JobExecutionException(exception);
    }
    finally {
      stopTime = new Date();

      if (status.equals(JobStatus.FAILURE) || (count > 0) || logOnZeroCount()) {
        LoggerManager.getLogger(this.getClass()).info("Job(%s) start(%s) stop(%s) count(%d) state(%s)", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
      }
    }
  }
}
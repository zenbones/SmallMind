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
package org.smallmind.schedule.quartz;

import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.smallmind.nutsnbolts.util.SuccessOrFailure;
import org.smallmind.schedule.base.ProxyJob;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Base Quartz job that wraps {@link ProxyJob} semantics with Quartz's
 * {@link InterruptableJob} lifecycle. The implementation tracks execution
 * timing, success/failure state, processed count, and any thrown errors,
 * logging outcomes on completion.
 */
public abstract class QuartzProxyJob implements ProxyJob, InterruptableJob {

  private final AtomicReference<Thread> threadRef = new AtomicReference<>();
  private final AtomicReference<SuccessOrFailure> statusRef = new AtomicReference<>(SuccessOrFailure.SUCCESS);
  private final LinkedList<Throwable> throwableList;

  private Date startTime;
  private Date stopTime;
  private int count = 0;

  /**
   * Creates a new proxy job with initial success status and empty error
   * collection.
   */
  public QuartzProxyJob () {

    throwableList = new LinkedList<>();
  }

  /**
   * Default enabled flag. Subclasses can override to provide conditional
   * enablement logic.
   *
   * @return {@code true} indicating the job should run
   */
  @Override
  public boolean isEnabled () {

    return true;
  }

  /**
   * Current execution status. Defaults to {@link SuccessOrFailure#SUCCESS}
   * and is updated when errors or interruptions occur.
   *
   * @return the job status for the most recent execution
   */
  @Override
  public SuccessOrFailure getJobStatus () {

    return statusRef.get();
  }

  /**
   * Timestamp when execution began.
   *
   * @return start time, or {@code null} if the job has not run
   */
  @Override
  public Date getStartTime () {

    return startTime;
  }

  /**
   * Timestamp when execution completed.
   *
   * @return stop time, or {@code null} if the job has not run
   */
  @Override
  public Date getStopTime () {

    return stopTime;
  }

  /**
   * Increment the processed count by one in a thread-safe manner.
   */
  @Override
  public synchronized void incCount () {

    count++;
  }

  /**
   * Increment the processed count by the provided value.
   *
   * @param additional number of units to add to the current count
   */
  public synchronized void addToCount (int additional) {

    count += additional;
  }

  /**
   * Retrieves the processed count captured for the execution.
   *
   * @return current count value
   */
  @Override
  public synchronized int getCount () {

    return count;
  }

  /**
   * Returns any errors recorded during execution.
   *
   * @return array of {@link Throwable}s or {@code null} when none exist
   */
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

  /**
   * Records a throwable and marks the job as failed.
   *
   * @param throwable the error encountered during execution
   */
  @Override
  public synchronized void setThrowable (Throwable throwable) {

    setThrowable(throwable, true);
  }

  /**
   * Records a throwable and optionally marks the job as failed. All recorded
   * errors are logged immediately.
   *
   * @param throwable the error encountered during execution
   * @param isFailure {@code true} to mark job status as {@link SuccessOrFailure#FAILURE}
   */
  public synchronized void setThrowable (Throwable throwable, boolean isFailure) {

    throwableList.add(throwable);

    if (isFailure) {
      statusRef.set(SuccessOrFailure.FAILURE);
    }

    LoggerManager.getLogger(this.getClass()).error(throwable);
  }

  /**
   * Attempts to interrupt the executing thread for this job when Quartz
   * requests cancellation.
   */
  @Override
  public void interrupt () {

    Thread thread;

    if ((thread = threadRef.get()) != null) {
      thread.interrupt();
    }
  }

  /**
   * Quartz entry point. Captures start/stop timestamps, tracks the executing
   * thread to support interruption, delegates to {@link #proceed()}, and logs
   * outcome information. Errors are recorded via {@link #setThrowable(Throwable)}
   * and cleanup is attempted in all cases.
   *
   * @param jobExecutionContext Quartz execution context for the fired trigger
   */
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
          LoggerManager.getLogger(this.getClass()).log(SuccessOrFailure.FAILURE.equals(status) ? Level.WARN : Level.INFO, "Job(%s) start(%s) stop(%s) count(%d) state(%s)", this.getClass().getSimpleName(), startTime, stopTime, count, status.name());
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

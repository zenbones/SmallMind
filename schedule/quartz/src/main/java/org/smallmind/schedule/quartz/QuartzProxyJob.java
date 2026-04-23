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

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.smallmind.nutsnbolts.util.SuccessOrFailure;
import org.smallmind.schedule.base.ProxyJob;
import org.smallmind.scribe.pen.Level;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Abstract Quartz job that adapts {@link ProxyJob} semantics to Quartz's
 * {@link InterruptableJob} lifecycle. Manages execution timing, thread
 * registration for interrupt support, status tracking, and structured
 * completion logging. Subclasses implement only {@link #proceed()} and
 * optionally {@link #cleanup()}.
 */
public abstract class QuartzProxyJob implements ProxyJob, InterruptableJob {

  private final AtomicReference<Thread> threadRef = new AtomicReference<>();
  private final AtomicReference<SuccessOrFailure> statusRef = new AtomicReference<>(SuccessOrFailure.SUCCESS);
  private final LinkedList<Throwable> throwableList;

  private LocalDateTime startTime;
  private LocalDateTime stopTime;
  private int count = 0;

  /**
   * Initializes state to {@link SuccessOrFailure#SUCCESS} with an empty error list.
   */
  public QuartzProxyJob () {

    throwableList = new LinkedList<>();
  }

  /**
   * Returns {@code true} unconditionally. Override to gate execution on
   * external configuration or runtime conditions.
   *
   * @return {@code true}, indicating the job should run by default
   */
  @Override
  public boolean isEnabled () {

    return true;
  }

  /**
   * Current job outcome, initialized to {@link SuccessOrFailure#SUCCESS} and
   * updated if errors or an interrupt are encountered during execution.
   *
   * @return the most recently recorded {@link SuccessOrFailure} value
   */
  @Override
  public SuccessOrFailure getJobStatus () {

    return statusRef.get();
  }

  /**
   * Time at which the most recent {@link #execute(JobExecutionContext)} call began.
   *
   * @return start timestamp, or {@code null} if the job has not yet run
   */
  @Override
  public LocalDateTime getStartTime () {

    return startTime;
  }

  /**
   * Time at which the most recent {@link #execute(JobExecutionContext)} call finished.
   *
   * @return stop timestamp, or {@code null} if the job has not yet run
   */
  @Override
  public LocalDateTime getStopTime () {

    return stopTime;
  }

  /**
   * Atomically increments the processed-item count by one.
   */
  @Override
  public synchronized void incCount () {

    count++;
  }

  /**
   * Adds an arbitrary amount to the processed-item count in a single operation.
   *
   * @param additional number of items to add; must be non-negative
   */
  public synchronized void addToCount (int additional) {

    count += additional;
  }

  /**
   * Current processed-item count for the running or most recent execution.
   *
   * @return item count
   */
  @Override
  public synchronized int getCount () {

    return count;
  }

  /**
   * Errors recorded during the current or most recent execution.
   *
   * @return array of {@link Throwable}s, or {@code null} if no errors were recorded
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
   * Records an error and unconditionally marks this job as
   * {@link SuccessOrFailure#FAILURE}. Delegates to
   * {@link #setThrowable(Throwable, boolean)} with {@code isFailure = true}.
   *
   * @param throwable error to record
   */
  @Override
  public synchronized void setThrowable (Throwable throwable) {

    setThrowable(throwable, true);
  }

  /**
   * Records an error and optionally marks this job as failed. The error is
   * logged immediately regardless of the {@code isFailure} flag.
   *
   * @param throwable the error to record
   * @param isFailure {@code true} to transition status to {@link SuccessOrFailure#FAILURE}
   */
  public synchronized void setThrowable (Throwable throwable, boolean isFailure) {

    throwableList.add(throwable);

    if (isFailure) {
      statusRef.set(SuccessOrFailure.FAILURE);
    }

    LoggerManager.getLogger(this.getClass()).error(throwable);
  }

  /**
   * Interrupts the thread currently executing this job, if one is registered.
   * Has no effect when the job is not actively running.
   */
  @Override
  public void interrupt () {

    Thread thread;

    if ((thread = threadRef.get()) != null) {
      thread.interrupt();
    }
  }

  /**
   * Quartz entry point. Records start and stop times, registers the executing
   * thread so that {@link #interrupt()} can reach it, delegates to
   * {@link #proceed()}, and logs a structured completion message. An
   * {@link InterruptedException} transitions status to
   * {@link SuccessOrFailure#INTERRUPTED}; any other exception is forwarded to
   * {@link #setThrowable(Throwable)}. {@link #cleanup()} is always invoked in
   * the finally block, and any exception it raises is logged without
   * propagation.
   *
   * @param jobExecutionContext context provided by Quartz for the fired trigger
   */
  @Override
  public void execute (JobExecutionContext jobExecutionContext) {

    if (isEnabled()) {
      startTime = LocalDateTime.now();

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
        stopTime = LocalDateTime.now();
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

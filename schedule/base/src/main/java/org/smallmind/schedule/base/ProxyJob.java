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
package org.smallmind.schedule.base;

import java.util.Date;
import org.smallmind.nutsnbolts.util.SuccessOrFailure;

/**
 * Contract for jobs that are executed by a scheduler while exposing minimal
 * lifecycle and bookkeeping hooks. Implementations typically wrap the real
 * job logic with cross-cutting concerns such as logging, status tracking,
 * and error collection.
 */
public interface ProxyJob {

  /**
   * Indicates whether the job should execute. Schedulers call this before
   * running {@link #proceed()} and can skip execution when disabled.
   *
   * @return {@code true} if the job should run, {@code false} to skip
   */
  boolean isEnabled ();

  /**
   * Signals whether the scheduler should log completion even when no work
   * has been recorded via {@link #incCount()}. Implementations that
   * consider zero-count executions noteworthy can return {@code true}.
   *
   * @return {@code true} to log when {@link #getCount()} is zero
   */
  boolean logOnZeroCount ();

  /**
   * Current execution status for the job, reflecting successes, failures,
   * or interruptions.
   *
   * @return the most recent {@link SuccessOrFailure} value
   */
  SuccessOrFailure getJobStatus ();

  /**
   * Number of work items processed during the current execution.
   *
   * @return the recorded count
   */
  int getCount ();

  /**
   * Increment the count of processed work items by one.
   */
  void incCount ();

  /**
   * Start time of the current or last execution.
   *
   * @return the start {@link Date}, or {@code null} if not yet executed
   */
  Date getStartTime ();

  /**
   * Stop time of the current or last execution.
   *
   * @return the stop {@link Date}, or {@code null} if not yet executed
   */
  Date getStopTime ();

  /**
   * Collection of errors encountered during execution. Implementations may
   * return {@code null} when no errors have been recorded.
   *
   * @return an array of recorded {@link Throwable}s, or {@code null} when none
   */
  Throwable[] getThrowables ();

  /**
   * Record a throwable encountered during execution and optionally update
   * job status to reflect failure.
   *
   * @param throwable the error to record
   */
  void setThrowable (Throwable throwable);

  /**
   * Core execution logic for the job. Implementations perform their work
   * here and should update counters, status, or throwables as appropriate.
   *
   * @throws Exception if execution fails or must be aborted
   */
  void proceed ()
    throws Exception;

  /**
   * Cleanup hook invoked after execution completes, regardless of success.
   *
   * @throws Exception if cleanup fails
   */
  void cleanup ()
    throws Exception;
}

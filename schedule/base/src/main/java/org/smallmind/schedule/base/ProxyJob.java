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

import java.time.LocalDateTime;
import org.smallmind.nutsnbolts.util.SuccessOrFailure;

/**
 * Defines the contract for a schedulable unit of work. Beyond the execution
 * method itself, the interface exposes the bookkeeping state — count, timing,
 * status, and errors — that a host scheduler records and logs for each firing.
 */
public interface ProxyJob {

  /**
   * Whether this job is eligible to execute. The scheduler evaluates this
   * before every firing and silently skips execution when {@code false} is
   * returned.
   *
   * @return {@code true} if execution should proceed
   */
  boolean isEnabled ();

  /**
   * Whether a zero processed-item count should still produce a completion log
   * entry. Implementations that consider empty-run diagnostics worthwhile
   * should return {@code true}.
   *
   * @return {@code true} to log completions even when {@link #getCount()} is zero
   */
  boolean logOnZeroCount ();

  /**
   * Outcome recorded for the most recent execution.
   *
   * @return a {@link SuccessOrFailure} value reflecting current job state
   */
  SuccessOrFailure getJobStatus ();

  /**
   * Number of items processed during the current or most recent execution.
   *
   * @return the running item count
   */
  int getCount ();

  /**
   * Increment the processed-item count by one.
   */
  void incCount ();

  /**
   * Timestamp marking the start of the current or most recent execution.
   *
   * @return start time as a {@link LocalDateTime}, or {@code null} if never executed
   */
  LocalDateTime getStartTime ();

  /**
   * Timestamp marking the end of the current or most recent execution.
   *
   * @return stop time as a {@link LocalDateTime}, or {@code null} if never executed
   */
  LocalDateTime getStopTime ();

  /**
   * Errors accumulated during the current or most recent execution.
   *
   * @return array of recorded {@link Throwable}s, or {@code null} if no errors were recorded
   */
  Throwable[] getThrowables ();

  /**
   * Record an error that occurred during execution.
   *
   * @param throwable the error to store
   */
  void setThrowable (Throwable throwable);

  /**
   * Execute the job's primary work. Implementations are responsible for
   * advancing {@link #incCount()} and calling {@link #setThrowable(Throwable)}
   * as appropriate.
   *
   * @throws Exception if execution fails and cannot continue
   */
  void proceed ()
    throws Exception;

  /**
   * Release resources or perform any post-execution housekeeping. Called by
   * the scheduler after {@link #proceed()} returns, whether or not it threw.
   *
   * @throws Exception if cleanup fails
   */
  void cleanup ()
    throws Exception;
}

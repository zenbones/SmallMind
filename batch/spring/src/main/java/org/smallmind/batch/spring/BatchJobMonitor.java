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
package org.smallmind.batch.spring;

import java.util.concurrent.TimeUnit;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Polls a Spring Batch job execution until it reaches one of the caller-supplied
 * {@link ExitStatus} values or a timeout expires.
 */
public class BatchJobMonitor {

  private final JobRepository jobRepository;
  private final Long jobId;

  /**
   * Creates a monitor targeting a specific job execution.
   *
   * @param jobRepository Spring Batch repository used to fetch execution state
   * @param jobId         the execution id to monitor
   */
  public BatchJobMonitor (JobRepository jobRepository, Long jobId) {

    this.jobRepository = jobRepository;
    this.jobId = jobId;
  }

  /**
   * Waits for the monitored execution to match any of the given exit statuses.
   * <p>
   * Polls at an interval of at most one-tenth of the total timeout, with a minimum of one
   * second. Returns {@code true} immediately if {@code exitStatuses} is empty or {@code null}.
   *
   * @param timeout      the maximum time to wait
   * @param timeUnit     the unit for {@code timeout}
   * @param exitStatuses acceptable terminal statuses; varargs, may be empty
   * @return {@code true} if a matching status was observed before the deadline, {@code false} otherwise
   * @throws NoSuchJobException   if the execution identified by the stored job id no longer exists
   * @throws InterruptedException if the monitoring thread is interrupted while sleeping
   */
  public boolean await (long timeout, TimeUnit timeUnit, ExitStatus... exitStatuses)
    throws NoSuchJobException, InterruptedException {

    if ((exitStatuses == null) || (exitStatuses.length == 0)) {

      return true;
    } else {

      long total = timeUnit.toMillis(timeout);
      long end = System.currentTimeMillis() + total;
      long pulse = Math.max(1000, total / 10);
      long remaining;

      while ((remaining = end - System.currentTimeMillis()) > 0) {

        JobExecution jobExecution;

        if ((jobExecution = jobRepository.getJobExecution(jobId)) == null) {
          throw new FormattedNoSuchJobException("No such job(%d)", jobId);
        } else {

          ExitStatus exitStatus = jobExecution.getExitStatus();

          for (ExitStatus acceptableExitStatus : exitStatuses) {
            if (acceptableExitStatus.equals(exitStatus)) {

              return true;
            }
          }
        }

        Thread.sleep(Math.min(pulse, remaining));
      }

      return false;
    }
  }
}

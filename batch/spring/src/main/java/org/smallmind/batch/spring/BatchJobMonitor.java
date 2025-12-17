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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;

/**
 * Utility for polling a Spring Batch job execution until a desired {@link ExitStatus} is observed.
 */
public class BatchJobMonitor {

  private final JobExplorer jobExplorer;
  private final Long jobId;

  /**
   * Creates a monitor for a specific job id.
   *
   * @param jobExplorer explorer used to query job executions
   * @param jobId       the job id to monitor
   */
  public BatchJobMonitor (JobExplorer jobExplorer, Long jobId) {

    this.jobExplorer = jobExplorer;
    this.jobId = jobId;
  }

  /**
   * Waits for the job to reach any of the supplied exit statuses within the timeout.
   *
   * @param timeout      the maximum time to wait
   * @param timeUnit     the time unit for the timeout value
   * @param exitStatuses acceptable terminal statuses; if none are provided the method returns immediately
   * @return {@code true} if an acceptable exit status was reached before timing out, otherwise {@code false}
   * @throws InterruptedException if the monitoring thread is interrupted while waiting
   * @throws MissingJobException  if the job execution cannot be found
   */
  public boolean await (long timeout, TimeUnit timeUnit, ExitStatus... exitStatuses)
    throws InterruptedException {

    if ((exitStatuses == null) || (exitStatuses.length == 0)) {

      return true;
    } else {

      long total = timeUnit.toMillis(timeout);
      long end = System.currentTimeMillis() + total;
      long pulse = Math.max(1000, Math.min(10, total / 10));
      long remaining;

      while ((remaining = end - System.currentTimeMillis()) > 0) {

        JobExecution jobExecution;

        if ((jobExecution = jobExplorer.getJobExecution(jobId)) == null) {
          throw new MissingJobException("No such job(%d)", jobId);
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

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
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Waits for all jobs visible through a {@link JobRepository} to reach {@link BatchStatus#COMPLETED}.
 */
public class BatchJobWatcher {

  private final JobRepository jobRepository;

  /**
   * Builds a watcher against the provided repository.
   *
   * @param jobRepository repository used to inspect running and completed jobs
   */
  public BatchJobWatcher (JobRepository jobRepository) {

    this.jobRepository = jobRepository;
  }

  /**
   * Polls until all jobs are complete for a minimum clear period or until timeout is reached.
   *
   * @param clear minimum duration jobs must remain complete before success is reported
   * @param timeout maximum wait duration before failing
   * @param timeUnit time unit for {@code clear} and {@code timeout}
   * @return {@code true} if all jobs remained complete for the clear period before timeout; otherwise {@code false}
   * @throws InterruptedException if the polling thread is interrupted while sleeping
   */
  public boolean await (long clear, long timeout, TimeUnit timeUnit)
    throws InterruptedException {

    long cleared = timeUnit.toMillis(clear);
    long total = timeUnit.toMillis(timeout);
    long checked = System.currentTimeMillis();
    long end = checked + total;
    long pulse = Math.max(1000, total / 10);
    long remaining;

    while ((remaining = end - System.currentTimeMillis()) > 0) {

      if (hasCompleted() && ((System.currentTimeMillis() - checked) >= cleared)) {

        return true;
      } else {
        checked = System.currentTimeMillis();
      }

      Thread.sleep(Math.min(pulse, remaining));
    }

    return false;
  }

  /**
   * Checks whether every known execution has completed.
   *
   * @return {@code true} when no execution is found in a non-completed state
   */
  private boolean hasCompleted () {

    for (String jobName : jobRepository.getJobNames()) {
      for (JobInstance jobInstance : jobRepository.findJobInstances(jobName)) {
        for (JobExecution jobExecution : jobInstance.getJobExecutions()) {
          if (!BatchStatus.COMPLETED.equals(jobExecution.getStatus())) {

            return false;
          }
        }
      }
    }

    return true;
  }
}

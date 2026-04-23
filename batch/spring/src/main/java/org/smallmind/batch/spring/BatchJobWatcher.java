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
 * Waits for every job execution visible through a {@link JobRepository} to reach
 * {@link BatchStatus#COMPLETED}.
 * <p>
 * An additional <em>clear</em> period prevents premature success: all jobs must have been
 * complete for at least the specified duration before the watcher declares victory.
 */
public class BatchJobWatcher {

  private final JobRepository jobRepository;

  /**
   * Creates a watcher backed by the provided repository.
   *
   * @param jobRepository the repository whose jobs are monitored
   */
  public BatchJobWatcher (JobRepository jobRepository) {

    this.jobRepository = jobRepository;
  }

  /**
   * Blocks until all jobs have been continuously complete for the {@code clear} period, or
   * until {@code timeout} elapses.
   * <p>
   * Polls at an interval of at most one-tenth of the total timeout with a minimum of one second.
   *
   * @param clear    minimum duration for which all jobs must remain complete before success is reported
   * @param timeout  maximum time to wait before returning {@code false}
   * @param timeUnit unit for both {@code clear} and {@code timeout}
   * @return {@code true} if all jobs stayed complete for the clear period before the deadline,
   * {@code false} if the timeout expired first
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
   * Iterates every execution across every known job instance and returns {@code false} as soon
   * as any execution has a status other than {@link BatchStatus#COMPLETED}.
   *
   * @return {@code true} only when every execution is in the completed state
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

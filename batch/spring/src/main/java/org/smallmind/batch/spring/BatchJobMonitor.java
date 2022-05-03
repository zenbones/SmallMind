/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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

public class BatchJobMonitor {

  private final JobExplorer jobExplorer;
  private final Long jobId;

  public BatchJobMonitor (JobExplorer jobExplorer, Long jobId) {

    this.jobExplorer = jobExplorer;
    this.jobId = jobId;
  }

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

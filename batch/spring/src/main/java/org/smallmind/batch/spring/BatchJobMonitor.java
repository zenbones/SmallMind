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

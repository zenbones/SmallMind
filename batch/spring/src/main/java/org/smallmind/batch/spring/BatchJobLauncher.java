/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019 David Berkman
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.TaskRejectedException;

public class BatchJobLauncher implements JobLauncher, ApplicationListener<ContextRefreshedEvent> {

  private JobRepository jobRepository;
  private ExecutorService taskExecutor = Executors.newSingleThreadExecutor();

  @Override
  public void onApplicationEvent (ContextRefreshedEvent event) {

    jobRepository = event.getApplicationContext().getBean("jobRepository", JobRepository.class);
  }

  @Override
  public JobExecution run (Job job, JobParameters jobParameters)
    throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {

    JobExecution jobExecution;
    JobExecution lastExecution = jobRepository.getLastJobExecution(job.getName(), jobParameters);

    if (lastExecution != null) {
      if (!job.isRestartable()) {
        throw new JobRestartException("JobInstance already exists and is not restartable");
      }
      /*
       * validate here if it has stepExecutions that are UNKNOWN, STARTING, STARTED and STOPPING
       * retrieve the previous execution and check
       */
      for (StepExecution execution : lastExecution.getStepExecutions()) {
        BatchStatus status = execution.getStatus();
        if (status.isRunning() || status == BatchStatus.STOPPING) {
          throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
                                                          + lastExecution);
        } else if (status == BatchStatus.UNKNOWN) {
          throw new JobRestartException(
            "Cannot restart step [" + execution.getStepName() + "] from UNKNOWN status. "
              + "The last execution ended with a failure that could not be rolled back, "
              + "so it may be dangerous to proceed. Manual intervention is probably necessary.");
        }
      }
    }

    // Check the validity of the parameters before doing creating anything
    // in the repository...
    job.getJobParametersValidator().validate(jobParameters);

    /*
     * There is a very small probability that a non-restartable job can be
     * restarted, but only if another process or thread manages to launch
     * <i>and</i> fail a job execution for this instance between the last
     * assertion and the next method returning successfully.
     */
    jobExecution = jobRepository.createJobExecution(job.getName(), jobParameters);

    try {
      taskExecutor.execute(new Runnable() {

        @Override
        public void run () {

          try {
            LoggerManager.getLogger(BatchJobLauncher.class).info("Job: [" + job + "] launched with the following parameters: [" + jobParameters + "]");

            job.execute(jobExecution);

            LoggerManager.getLogger(BatchJobLauncher.class).info("Job: [" + job + "] completed with the following parameters: [" + jobParameters + "] and the following status: [" + jobExecution.getStatus() + "]");
          } catch (Throwable t) {
            LoggerManager.getLogger(BatchJobLauncher.class).info("Job: [" + job + "] failed unexpectedly and fatally with the following parameters: [" + jobParameters + "]", t);
            rethrow(t);
          }
        }

        private void rethrow (Throwable t) {

          if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
          } else if (t instanceof Error) {
            throw (Error)t;
          }
          throw new IllegalStateException(t);
        }
      });
    } catch (TaskRejectedException e) {
      jobExecution.upgradeStatus(BatchStatus.FAILED);
      if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
        jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
      }
      jobRepository.update(jobExecution);
    }

    return jobExecution;
  }
}



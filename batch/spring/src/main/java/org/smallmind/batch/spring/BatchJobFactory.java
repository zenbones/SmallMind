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

import java.util.Map;
import org.smallmind.batch.base.BatchParameter;
import org.smallmind.batch.base.DateBatchParameter;
import org.smallmind.batch.base.DoubleBatchParameter;
import org.smallmind.batch.base.JobFactory;
import org.smallmind.batch.base.LongBatchParameter;
import org.smallmind.batch.base.StringBatchParameter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.smallmind.scribe.pen.LoggerManager;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;

/**
 * Spring Batch backed implementation of {@link org.smallmind.batch.base.JobFactory} that translates the project level
 * parameter wrappers into Spring {@link org.springframework.batch.core.job.parameters.JobParameters} and launches jobs.
 */
public class BatchJobFactory implements JobFactory {

  private JobRepository jobRepository;
  private JobRegistry jobRegistry;
  private JobOperator jobOperator;

  /**
   * Injects the {@link JobRepository} used to query historical executions.
   *
   * @param jobRepository the repository to use
   */
  public void setJobRepository (JobRepository jobRepository) {

    this.jobRepository = jobRepository;
  }

  /**
   * Injects the {@link JobRegistry} used to resolve jobs by logical name.
   *
   * @param jobRegistry the registry to use
   */
  public void setJobRegistry (JobRegistry jobRegistry) {

    this.jobRegistry = jobRegistry;
  }

  /**
   * Injects the {@link JobOperator} used to restart jobs.
   *
   * @param jobOperator the operator to use
   */
  public void setJobOperator (JobOperator jobOperator) {

    this.jobOperator = jobOperator;
  }

  /**
   * Launches a job while logging the invocation reason and parameters.
   *
   * @param logicalName  the logical job name
   * @param parameterMap parameters supplied to the job
   * @param reason       optional text describing why the job is starting (used only for logging)
   * @return the id of the resulting job execution
   * @throws NoSuchJobException                  if the logical name cannot be resolved
   * @throws InvalidJobParametersException       if required parameters are missing or invalid
   * @throws JobExecutionAlreadyRunningException if an execution of the job is already running and cannot overlap
   * @throws JobRestartException                 if a restartable job cannot be restarted
   * @throws JobInstanceAlreadyCompleteException if the specified parameters map to a job instance that already completed
   */
  @Override
  public Long create (String logicalName, Map<String, BatchParameter<?>> parameterMap, String reason)
    throws NoSuchJobException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    LoggerManager.getLogger(BatchJobFactory.class).info("Batch(%s) for reason(%s) with parameters(%s)", logicalName, (reason == null) ? "unknown" : reason, parametersAsString(parameterMap));

    return start(logicalName, parameterMap);
  }

  /**
   * Produces a readable representation of the parameter map for logging.
   *
   * @param parameterMap the parameters to describe
   * @return a formatted string describing the parameters, or {@code "none"} if the map is empty
   */
  private String parametersAsString (Map<String, BatchParameter<?>> parameterMap) {

    if ((parameterMap == null) || parameterMap.isEmpty()) {

      return "none";
    } else {

      StringBuilder parameterBuilder = new StringBuilder("{");
      boolean first = true;

      for (Map.Entry<String, BatchParameter<?>> parameterEntry : parameterMap.entrySet()) {
        if (!first) {
          parameterBuilder.append(',');
        }
        parameterBuilder.append(parameterEntry.getKey()).append(": [type=").append(parameterEntry.getValue().getType()).append(",value=").append(parameterEntry.getValue().getValue()).append(",identifying=").append(parameterEntry.getValue().isIdentifying()).append(']');
        first = false;
      }

      return parameterBuilder.append('}').toString();
    }
  }

  /**
   * Restarts a previously executed job by id.
   *
   * @param executionId the job execution id
   * @throws NoSuchJobException  if the job cannot be found
   * @throws JobRestartException if the job cannot be restarted
   */
  @Override
  public void restart (long executionId)
    throws NoSuchJobException, JobRestartException {

    JobExecution jobExecution;

    if ((jobExecution = jobRepository.getJobExecution(executionId)) == null) {
      throw new FormattedJobRestartException("Could not locate job execution(%d)", executionId);
    } else {
      jobOperator.restart(jobExecution);
    }
  }

  /**
   * Creates a monitor for the given job id.
   *
   * @param jobId the job id to monitor
   * @return a monitor that can await specific exit states
   */
  public BatchJobMonitor monitor (Long jobId) {

    return new BatchJobMonitor(jobRepository, jobId);
  }

  /**
   * Creates a monitor for the most recent execution of the named job.
   *
   * @param logicalName the job name
   * @return a monitor targeting the latest job execution
   * @throws NoSuchJobException if no job instance or execution can be found
   */
  public BatchJobMonitor monitorLatest (String logicalName)
    throws NoSuchJobException {

    JobInstance jobInstance;

    if ((jobInstance = jobRepository.getLastJobInstance(logicalName)) == null) {
      throw new FormattedNoSuchJobException("Missing job instance(%s)", logicalName);
    } else {

      JobExecution jobExecution;

      if ((jobExecution = jobRepository.getLastJobExecution(jobInstance)) == null) {
        throw new FormattedNoSuchJobException("Missing job execution(%s)", logicalName);
      } else {

        return new BatchJobMonitor(jobRepository, jobExecution.getId());
      }
    }
  }

  /**
   * Creates a watcher that can block until all known Spring Batch jobs finish.
   *
   * @return a watcher bound to this factory's {@link JobRepository}
   */
  public BatchJobWatcher watch () {

    return new BatchJobWatcher(jobRepository);
  }

  /**
   * Launches a job with the supplied parameters.
   *
   * @param logicalName  the logical job name to resolve
   * @param parameterMap the parameters to pass to the job
   * @return the id of the created job execution
   * @throws NoSuchJobException                  if the job name cannot be resolved
   * @throws InvalidJobParametersException       if the parameter map fails Spring Batch validation
   * @throws JobExecutionAlreadyRunningException if the job is already running with the same parameters
   * @throws JobRestartException                 if the job cannot be restarted with the provided parameters
   * @throws JobInstanceAlreadyCompleteException if the parameters map to a completed job instance
   */
  public long start (String logicalName, Map<String, BatchParameter<?>> parameterMap)
    throws NoSuchJobException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    Job job;
    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

    if (parameterMap != null) {
      for (Map.Entry<String, BatchParameter<?>> parameterEntry : parameterMap.entrySet()) {
        switch (parameterEntry.getValue().getType()) {
          case DATE:
            jobParametersBuilder.addLocalDateTime(parameterEntry.getKey(), ((DateBatchParameter)parameterEntry.getValue()).getValue(), parameterEntry.getValue().isIdentifying());
            break;
          case DOUBLE:
            jobParametersBuilder.addDouble(parameterEntry.getKey(), ((DoubleBatchParameter)parameterEntry.getValue()).getValue(), parameterEntry.getValue().isIdentifying());
            break;
          case LONG:
            jobParametersBuilder.addLong(parameterEntry.getKey(), ((LongBatchParameter)parameterEntry.getValue()).getValue(), parameterEntry.getValue().isIdentifying());
            break;
          case STRING:
            jobParametersBuilder.addString(parameterEntry.getKey(), ((StringBatchParameter)parameterEntry.getValue()).getValue(), parameterEntry.getValue().isIdentifying());
            break;
          default:
            throw new UnknownSwitchCaseException(parameterEntry.getValue().getType().name());
        }
      }
    }

    if ((job = jobRegistry.getJob(logicalName)) == null) {
      throw new FormattedNoSuchJobException("Could not locate job(%s)", logicalName);
    } else {

      return jobOperator.start(job, jobParametersBuilder.toJobParameters()).getId();
    }
  }
}

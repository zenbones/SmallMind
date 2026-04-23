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
 * Spring Batch implementation of {@link org.smallmind.batch.base.JobFactory}.
 * <p>
 * Converts framework-neutral {@link org.smallmind.batch.base.BatchParameter} instances to Spring
 * {@link org.springframework.batch.core.job.parameters.JobParameters}, then delegates to a
 * {@link org.springframework.batch.core.launch.JobOperator} to start or restart jobs. Also
 * vends {@link BatchJobMonitor} and {@link BatchJobWatcher} instances for polling job state.
 */
public class BatchJobFactory implements JobFactory {

  private JobRepository jobRepository;
  private JobRegistry jobRegistry;
  private JobOperator jobOperator;

  /**
   * Sets the repository used to query historical job executions.
   *
   * @param jobRepository the Spring Batch repository
   */
  public void setJobRepository (JobRepository jobRepository) {

    this.jobRepository = jobRepository;
  }

  /**
   * Sets the registry used to resolve job definitions by logical name.
   *
   * @param jobRegistry the Spring Batch job registry
   */
  public void setJobRegistry (JobRegistry jobRegistry) {

    this.jobRegistry = jobRegistry;
  }

  /**
   * Sets the operator used to start and restart jobs.
   *
   * @param jobOperator the Spring Batch job operator
   */
  public void setJobOperator (JobOperator jobOperator) {

    this.jobOperator = jobOperator;
  }

  /**
   * Logs the launch reason and parameters, then starts the named job.
   *
   * @param logicalName  logical name of the job to start
   * @param parameterMap parameters to supply; {@code null} is treated as empty
   * @param reason       optional launch reason written to the log; defaults to {@code "unknown"} when {@code null}
   * @return the id of the resulting job execution
   * @throws NoSuchJobException                  if {@code logicalName} cannot be resolved in the registry
   * @throws InvalidJobParametersException       if the translated parameters fail Spring Batch validation
   * @throws JobExecutionAlreadyRunningException if the same job instance is already executing
   * @throws JobRestartException                 if a restartable execution cannot be restarted with these parameters
   * @throws JobInstanceAlreadyCompleteException if the given parameters map to an already-completed instance
   */
  @Override
  public Long create (String logicalName, Map<String, BatchParameter<?>> parameterMap, String reason)
    throws NoSuchJobException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    LoggerManager.getLogger(BatchJobFactory.class).info("Batch(%s) for reason(%s) with parameters(%s)", logicalName, (reason == null) ? "unknown" : reason, parametersAsString(parameterMap));

    return start(logicalName, parameterMap);
  }

  /**
   * Formats the parameter map as a human-readable string for log output.
   *
   * @param parameterMap the parameters to describe; may be {@code null}
   * @return a brace-delimited entry list, or {@code "none"} if the map is absent or empty
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
   * Re-runs the job execution identified by {@code executionId}.
   *
   * @param executionId the id of the execution to restart
   * @throws NoSuchJobException  if no execution with the given id can be found
   * @throws JobRestartException if the operator cannot restart the execution
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
   * Returns a monitor that can poll the given job execution until a desired exit status is reached.
   *
   * @param jobId the execution id to monitor
   * @return a {@link BatchJobMonitor} targeting the specified execution
   */
  public BatchJobMonitor monitor (Long jobId) {

    return new BatchJobMonitor(jobRepository, jobId);
  }

  /**
   * Returns a monitor for the most recent execution of the named job.
   *
   * @param logicalName the job name to inspect
   * @return a {@link BatchJobMonitor} targeting the latest execution
   * @throws NoSuchJobException if no job instance or execution exists for {@code logicalName}
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
   * Returns a watcher that can block until all batch jobs visible to this factory reach
   * {@link org.springframework.batch.core.BatchStatus#COMPLETED}.
   *
   * @return a new {@link BatchJobWatcher} bound to this factory's repository
   */
  public BatchJobWatcher watch () {

    return new BatchJobWatcher(jobRepository);
  }

  /**
   * Translates the parameter map to Spring {@code JobParameters} and launches the job.
   *
   * @param logicalName  the registered job name
   * @param parameterMap the parameters to pass; {@code null} is treated as no parameters
   * @return the id of the new job execution
   * @throws NoSuchJobException                  if {@code logicalName} is not in the registry
   * @throws InvalidJobParametersException       if Spring Batch rejects the translated parameters
   * @throws JobExecutionAlreadyRunningException if a conflicting execution is already active
   * @throws JobRestartException                 if the job cannot be restarted with these parameters
   * @throws JobInstanceAlreadyCompleteException if the parameters identify a completed instance
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

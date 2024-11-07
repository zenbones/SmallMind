/*
 * Copyright (c) 2007 through 2024 David Berkman
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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

public class BatchJobFactory implements JobFactory {

  private JobLocator jobLocator;
  private JobLauncher jobLauncher;
  private JobOperator jobOperator;
  private JobExplorer jobExplorer;

  public void setJobLocator (JobLocator jobLocator) {

    this.jobLocator = jobLocator;
  }

  public void setJobLauncher (JobLauncher jobLauncher) {

    this.jobLauncher = jobLauncher;
  }

  public void setJobOperator (JobOperator jobOperator) {

    this.jobOperator = jobOperator;
  }

  public void setJobExplorer (JobExplorer jobExplorer) {

    this.jobExplorer = jobExplorer;
  }

  @Override
  public Long create (String logicalName, Map<String, BatchParameter<?>> parameterMap, String reason)
    throws NoSuchJobException, JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    LoggerManager.getLogger(BatchJobFactory.class).info("Batch(%s) for reason(%s) with parameters(%s)", logicalName, (reason == null) ? "unknown" : reason, parametersAsString(parameterMap));

    return start(logicalName, parameterMap);
  }

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

  @Override
  public void restart (long executionId)
    throws NoSuchJobException, NoSuchJobExecutionException, JobParametersInvalidException, JobRestartException, JobInstanceAlreadyCompleteException {

    jobOperator.restart(executionId);
  }

  public BatchJobMonitor monitor (Long jobId) {

    return new BatchJobMonitor(jobExplorer, jobId);
  }

  public BatchJobMonitor monitorLatest (String logicalName) {

    JobInstance jobInstance;

    if ((jobInstance = jobExplorer.getLastJobInstance(logicalName)) == null) {
      throw new MissingJobException("Missing job instance(%s)", logicalName);
    } else {

      JobExecution jobExecution;

      if ((jobExecution = jobExplorer.getLastJobExecution(jobInstance)) == null) {
        throw new MissingJobException("Missing job execution(%s)", logicalName);
      } else {

        return new BatchJobMonitor(jobExplorer, jobExecution.getJobId());
      }
    }
  }

  public Long start (String logicalName, Map<String, BatchParameter<?>> parameterMap)
    throws NoSuchJobException, JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

    if (parameterMap != null) {
      for (Map.Entry<String, BatchParameter<?>> parameterEntry : parameterMap.entrySet()) {
        switch (parameterEntry.getValue().getType()) {
          case DATE:
            jobParametersBuilder.addDate(parameterEntry.getKey(), ((DateBatchParameter)parameterEntry.getValue()).getValue(), parameterEntry.getValue().isIdentifying());
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

    return jobLauncher.run(jobLocator.getJob(logicalName), jobParametersBuilder.toJobParameters()).getJobId();
  }
}

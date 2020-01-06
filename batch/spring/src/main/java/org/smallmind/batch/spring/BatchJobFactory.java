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

import java.util.Map;
import org.smallmind.batch.base.BatchParameter;
import org.smallmind.batch.base.DateBatchParameter;
import org.smallmind.batch.base.DoubleBatchParameter;
import org.smallmind.batch.base.JobFactory;
import org.smallmind.batch.base.LongBatchParameter;
import org.smallmind.batch.base.StringBatchParameter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.configuration.JobLocator;
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

  public void setJobLocator (JobLocator jobLocator) {

    this.jobLocator = jobLocator;
  }

  public void setJobLauncher (JobLauncher jobLauncher) {

    this.jobLauncher = jobLauncher;
  }

  public void setJobOperator (JobOperator jobOperator) {

    this.jobOperator = jobOperator;
  }

  @Override
  public void create (String logicalName, Map<String, BatchParameter<?>> parameterMap)
    throws NoSuchJobException, JobParametersInvalidException, JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

    if (parameterMap != null) {
      for (Map.Entry<String, BatchParameter<?>> parameterEntry : parameterMap.entrySet()) {
        switch (parameterEntry.getValue().getType()) {
          case DATE:
            jobParametersBuilder.addDate(parameterEntry.getKey(), ((DateBatchParameter)parameterEntry.getValue()).getValue());
            break;
          case DOUBLE:
            jobParametersBuilder.addDouble(parameterEntry.getKey(), ((DoubleBatchParameter)parameterEntry.getValue()).getValue());
            break;
          case LONG:
            jobParametersBuilder.addLong(parameterEntry.getKey(), ((LongBatchParameter)parameterEntry.getValue()).getValue());
            break;
          case STRING:
            jobParametersBuilder.addString(parameterEntry.getKey(), ((StringBatchParameter)parameterEntry.getValue()).getValue());
            break;
          default:
            throw new UnknownSwitchCaseException(parameterEntry.getValue().getType().name());
        }
      }
    }

    jobLauncher.run(jobLocator.getJob(logicalName), jobParametersBuilder.toJobParameters());
  }

  @Override
  public void restart (long executionId)
    throws NoSuchJobException, NoSuchJobExecutionException, JobParametersInvalidException, JobRestartException, JobInstanceAlreadyCompleteException {

    jobOperator.restart(executionId);
  }
}

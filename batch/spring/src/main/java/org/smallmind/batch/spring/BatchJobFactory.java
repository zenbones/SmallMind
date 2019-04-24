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

import java.util.Map;
import org.smallmind.batch.base.DateProxyParameter;
import org.smallmind.batch.base.DoubleProxyParameter;
import org.smallmind.batch.base.JobFactory;
import org.smallmind.batch.base.LongProxyParameter;
import org.smallmind.batch.base.ProxyParameter;
import org.smallmind.batch.base.StringProxyParameter;
import org.smallmind.nutsnbolts.lang.UnknownSwitchCaseException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;

public class BatchJobFactory implements JobFactory {

  private JobRepository jobRepository;

  public void setJobRepository (JobRepository jobRepository) {

    this.jobRepository = jobRepository;
  }

  @Override
  public void create (String logicalName, Map<String, ProxyParameter<?>> parameterMap)
    throws JobExecutionAlreadyRunningException, JobRestartException, JobInstanceAlreadyCompleteException {

    JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();

    if (parameterMap != null) {
      for (Map.Entry<String, ProxyParameter<?>> parameterEntry : parameterMap.entrySet()) {
        switch (parameterEntry.getValue().getType()) {
          case DATE:
            jobParametersBuilder.addDate(parameterEntry.getKey(), ((DateProxyParameter)parameterEntry).getValue());
            break;
          case DOUBLE:
            jobParametersBuilder.addDouble(parameterEntry.getKey(), ((DoubleProxyParameter)parameterEntry).getValue());
            break;
          case LONG:
            jobParametersBuilder.addLong(parameterEntry.getKey(), ((LongProxyParameter)parameterEntry).getValue());
            break;
          case STRING:
            jobParametersBuilder.addString(parameterEntry.getKey(), ((StringProxyParameter)parameterEntry).getValue());
            break;
          default:
            throw new UnknownSwitchCaseException(parameterEntry.getValue().getType().name());
        }
      }
    }

    jobRepository.createJobExecution(logicalName, jobParametersBuilder.toJobParameters());
  }
}

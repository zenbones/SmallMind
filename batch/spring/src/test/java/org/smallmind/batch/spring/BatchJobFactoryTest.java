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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.smallmind.batch.base.BatchParameter;
import org.smallmind.batch.base.DateBatchParameter;
import org.smallmind.batch.base.DoubleBatchParameter;
import org.smallmind.batch.base.LongBatchParameter;
import org.smallmind.batch.base.StringBatchParameter;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BatchJobFactoryTest {

  private BatchJobFactory factoryWith (JobRepository jobRepository, JobRegistry jobRegistry, JobOperator jobOperator) {

    BatchJobFactory factory = new BatchJobFactory();

    factory.setJobRepository(jobRepository);
    factory.setJobRegistry(jobRegistry);
    factory.setJobOperator(jobOperator);

    return factory;
  }

  public void testStartTranslatesAllParameterTypesAndReturnsExecutionId ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);
    Job job = Mockito.mock(Job.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    LocalDateTime when = LocalDateTime.of(2026, 5, 16, 10, 0);
    Map<String, BatchParameter<?>> parameterMap = new HashMap<>();
    parameterMap.put("when", new DateBatchParameter(when, true));
    parameterMap.put("threshold", new DoubleBatchParameter(1.5d, false));
    parameterMap.put("count", new LongBatchParameter(7L, true));
    parameterMap.put("region", new StringBatchParameter("us-east-1", false));

    Mockito.when(jobRegistry.getJob("loader")).thenReturn(job);
    Mockito.when(jobExecution.getId()).thenReturn(99L);

    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);

    Mockito.when(jobOperator.start(Mockito.eq(job), captor.capture())).thenReturn(jobExecution);

    long execId = factoryWith(jobRepository, jobRegistry, jobOperator).start("loader", parameterMap);

    Assert.assertEquals(execId, 99L);

    JobParameters built = captor.getValue();

    Assert.assertEquals(built.getLocalDateTime("when"), when);
    Assert.assertEquals(Double.valueOf(built.getDouble("threshold")), Double.valueOf(1.5d));
    Assert.assertEquals(built.getLong("count"), Long.valueOf(7L));
    Assert.assertEquals(built.getString("region"), "us-east-1");
    Assert.assertTrue(built.getParameter("when").identifying());
    Assert.assertFalse(built.getParameter("threshold").identifying());
    Assert.assertTrue(built.getParameter("count").identifying());
    Assert.assertFalse(built.getParameter("region").identifying());
  }

  public void testStartWithNullParameterMapDelegatesWithEmptyParameters ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);
    Job job = Mockito.mock(Job.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    Mockito.when(jobRegistry.getJob("loader")).thenReturn(job);
    Mockito.when(jobExecution.getId()).thenReturn(11L);

    ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);

    Mockito.when(jobOperator.start(Mockito.eq(job), captor.capture())).thenReturn(jobExecution);

    long execId = factoryWith(jobRepository, jobRegistry, jobOperator).start("loader", null);

    Assert.assertEquals(execId, 11L);
    Assert.assertTrue(captor.getValue().isEmpty());
  }

  public void testStartThrowsWhenJobNotFoundInRegistry () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);

    Mockito.when(jobRegistry.getJob("missing")).thenReturn(null);

    try {
      factoryWith(jobRepository, jobRegistry, jobOperator).start("missing", null);
      Assert.fail("expected FormattedNoSuchJobException");
    } catch (NoSuchJobException expected) {
      Assert.assertTrue(expected instanceof FormattedNoSuchJobException);
      Assert.assertTrue(expected.getMessage().contains("missing"));
    } catch (Exception other) {
      Assert.fail("unexpected exception: " + other);
    }
  }

  public void testRestartLooksUpExecutionAndDelegatesToOperator ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    Mockito.when(jobRepository.getJobExecution(77L)).thenReturn(jobExecution);

    factoryWith(jobRepository, jobRegistry, jobOperator).restart(77L);

    Mockito.verify(jobOperator).restart(jobExecution);
  }

  public void testRestartThrowsWhenExecutionMissing () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);

    Mockito.when(jobRepository.getJobExecution(404L)).thenReturn(null);

    try {
      factoryWith(jobRepository, jobRegistry, jobOperator).restart(404L);
      Assert.fail("expected FormattedJobRestartException");
    } catch (Exception expected) {
      Assert.assertTrue(expected instanceof FormattedJobRestartException);
      Assert.assertTrue(expected.getMessage().contains("404"));
    }
  }

  public void testMonitorReturnsBoundMonitor () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);

    BatchJobMonitor monitor = factoryWith(jobRepository, jobRegistry, jobOperator).monitor(123L);

    Assert.assertNotNull(monitor);
  }

  public void testMonitorLatestThrowsWhenNoInstance () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);

    Mockito.when(jobRepository.getLastJobInstance("loader")).thenReturn(null);

    try {
      factoryWith(jobRepository, jobRegistry, jobOperator).monitorLatest("loader");
      Assert.fail("expected FormattedNoSuchJobException");
    } catch (NoSuchJobException expected) {
      Assert.assertTrue(expected instanceof FormattedNoSuchJobException);
      Assert.assertTrue(expected.getMessage().contains("loader"));
    }
  }

  public void testMonitorLatestThrowsWhenNoExecution () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);
    JobInstance jobInstance = Mockito.mock(JobInstance.class);

    Mockito.when(jobRepository.getLastJobInstance("loader")).thenReturn(jobInstance);
    Mockito.when(jobRepository.getLastJobExecution(jobInstance)).thenReturn(null);

    try {
      factoryWith(jobRepository, jobRegistry, jobOperator).monitorLatest("loader");
      Assert.fail("expected FormattedNoSuchJobException");
    } catch (NoSuchJobException expected) {
      Assert.assertTrue(expected instanceof FormattedNoSuchJobException);
    }
  }

  public void testMonitorLatestReturnsMonitorWhenExecutionPresent ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);
    JobInstance jobInstance = Mockito.mock(JobInstance.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    Mockito.when(jobRepository.getLastJobInstance("loader")).thenReturn(jobInstance);
    Mockito.when(jobRepository.getLastJobExecution(jobInstance)).thenReturn(jobExecution);
    Mockito.when(jobExecution.getId()).thenReturn(55L);

    BatchJobMonitor monitor = factoryWith(jobRepository, jobRegistry, jobOperator).monitorLatest("loader");

    Assert.assertNotNull(monitor);
  }

  public void testWatchReturnsWatcher () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobRegistry jobRegistry = Mockito.mock(JobRegistry.class);
    JobOperator jobOperator = Mockito.mock(JobOperator.class);

    Assert.assertNotNull(factoryWith(jobRepository, jobRegistry, jobOperator).watch());
  }
}

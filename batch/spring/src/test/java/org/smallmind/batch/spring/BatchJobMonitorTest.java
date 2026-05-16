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
import org.mockito.Mockito;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class BatchJobMonitorTest {

  public void testAwaitReturnsTrueImmediatelyWhenNoExitStatusesSupplied ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    BatchJobMonitor monitor = new BatchJobMonitor(jobRepository, 1L);

    Assert.assertTrue(monitor.await(50, TimeUnit.MILLISECONDS));
    Mockito.verifyNoInteractions(jobRepository);
  }

  public void testAwaitReturnsTrueWhenMatchingStatusObserved ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    Mockito.when(jobRepository.getJobExecution(1L)).thenReturn(jobExecution);
    Mockito.when(jobExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

    BatchJobMonitor monitor = new BatchJobMonitor(jobRepository, 1L);

    Assert.assertTrue(monitor.await(2, TimeUnit.SECONDS, ExitStatus.COMPLETED));
  }

  public void testAwaitReturnsFalseWhenTimeoutElapsesWithoutMatch ()
    throws Exception {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);
    JobExecution jobExecution = Mockito.mock(JobExecution.class);

    Mockito.when(jobRepository.getJobExecution(1L)).thenReturn(jobExecution);
    Mockito.when(jobExecution.getExitStatus()).thenReturn(ExitStatus.EXECUTING);

    BatchJobMonitor monitor = new BatchJobMonitor(jobRepository, 1L);

    Assert.assertFalse(monitor.await(100, TimeUnit.MILLISECONDS, ExitStatus.COMPLETED));
  }

  public void testAwaitThrowsWhenExecutionVanishes () {

    JobRepository jobRepository = Mockito.mock(JobRepository.class);

    Mockito.when(jobRepository.getJobExecution(1L)).thenReturn(null);

    BatchJobMonitor monitor = new BatchJobMonitor(jobRepository, 1L);

    try {
      monitor.await(500, TimeUnit.MILLISECONDS, ExitStatus.COMPLETED);
      Assert.fail("expected FormattedNoSuchJobException");
    } catch (NoSuchJobException expected) {
      Assert.assertTrue(expected instanceof FormattedNoSuchJobException);
    } catch (InterruptedException unexpected) {
      Assert.fail("unexpected interrupt: " + unexpected);
    }
  }
}

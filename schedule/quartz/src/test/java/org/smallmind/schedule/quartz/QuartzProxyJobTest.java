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
package org.smallmind.schedule.quartz;

import org.smallmind.nutsnbolts.util.SuccessOrFailure;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class QuartzProxyJobTest {

  /**
   * Concrete subclass whose {@code proceed()} and {@code cleanup()} behavior is
   * driven by fields so that each test can dial in the lifecycle path it cares
   * about. {@code execute(JobExecutionContext)} never touches the context, so
   * the tests pass {@code null}.
   */
  private static class HarnessJob extends QuartzProxyJob {

    private Runnable body;
    private Exception proceedFailure;
    private RuntimeException cleanupFailure;
    private boolean enabled = true;
    private boolean logZero = false;
    private boolean proceedRan = false;
    private boolean cleanupRan = false;

    @Override
    public boolean isEnabled () {

      return enabled;
    }

    @Override
    public boolean logOnZeroCount () {

      return logZero;
    }

    @Override
    public void proceed ()
      throws Exception {

      proceedRan = true;

      if (body != null) {
        body.run();
      }
      if (proceedFailure != null) {
        throw proceedFailure;
      }
    }

    @Override
    public void cleanup () {

      cleanupRan = true;

      if (cleanupFailure != null) {
        throw cleanupFailure;
      }
    }
  }

  public void testEnabledSuccessRecordsTimingAndRunsCleanup () {

    HarnessJob job = new HarnessJob();

    job.body = job::incCount;
    job.execute(null);

    Assert.assertTrue(job.proceedRan);
    Assert.assertTrue(job.cleanupRan);
    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
    Assert.assertNotNull(job.getStartTime());
    Assert.assertNotNull(job.getStopTime());
    Assert.assertEquals(job.getCount(), 1);
    Assert.assertNull(job.getThrowables());
  }

  public void testDisabledSkipsProceedCleanupAndTiming () {

    HarnessJob job = new HarnessJob();

    job.enabled = false;
    job.execute(null);

    Assert.assertFalse(job.proceedRan);
    Assert.assertFalse(job.cleanupRan);
    Assert.assertNull(job.getStartTime());
    Assert.assertNull(job.getStopTime());
    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
  }

  public void testProceedExceptionMarksFailureAndRecordsThrowable () {

    HarnessJob job = new HarnessJob();
    IllegalStateException boom = new IllegalStateException("boom");

    job.proceedFailure = boom;
    job.execute(null);

    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.FAILURE);
    Assert.assertNotNull(job.getThrowables());
    Assert.assertEquals(job.getThrowables().length, 1);
    Assert.assertSame(job.getThrowables()[0], boom);
    Assert.assertNotNull(job.getStopTime());
    Assert.assertTrue(job.cleanupRan);
  }

  public void testInterruptedExceptionMarksInterruptedWithoutRecordingThrowable () {

    HarnessJob job = new HarnessJob();

    job.proceedFailure = new InterruptedException();
    job.execute(null);

    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.INTERRUPTED);
    Assert.assertNull(job.getThrowables());
    Assert.assertTrue(job.cleanupRan);
  }

  public void testCleanupExceptionIsSwallowedAndLeavesStatusUnchanged () {

    HarnessJob job = new HarnessJob();

    job.cleanupFailure = new RuntimeException("cleanup blew up");
    job.execute(null);

    Assert.assertTrue(job.cleanupRan);
    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
  }

  public void testSetThrowableOneArgMarksFailure () {

    HarnessJob job = new HarnessJob();

    job.setThrowable(new RuntimeException("oops"));

    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.FAILURE);
    Assert.assertEquals(job.getThrowables().length, 1);
  }

  public void testSetThrowableTwoArgFalseRecordsWithoutFailure () {

    HarnessJob job = new HarnessJob();

    job.setThrowable(new RuntimeException("non-fatal"), false);

    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
    Assert.assertEquals(job.getThrowables().length, 1);
  }

  public void testGetThrowablesNullWhenNoneRecorded () {

    Assert.assertNull(new HarnessJob().getThrowables());
  }

  public void testCountIncrementAndAdd () {

    HarnessJob job = new HarnessJob();

    job.incCount();
    job.incCount();
    job.addToCount(5);

    Assert.assertEquals(job.getCount(), 7);
  }

  public void testInterruptBeforeFiringIsNoOp () {

    new HarnessJob().interrupt();

    Assert.assertFalse(Thread.currentThread().isInterrupted());
  }

  public void testInterruptDuringFiringReachesRegisteredThread () {

    HarnessJob job = new HarnessJob();
    boolean[] interruptedInsideRun = new boolean[1];

    // interrupt() targets the thread registered for the duration of execute();
    // calling it from within proceed() must interrupt the running thread. The
    // interrupted status is consumed here so it does not leak to other tests.
    job.body = () -> {
      job.interrupt();
      interruptedInsideRun[0] = Thread.interrupted();
    };
    job.execute(null);

    Assert.assertTrue(interruptedInsideRun[0]);
    Assert.assertFalse(Thread.currentThread().isInterrupted());
  }

  public void testDefaultIsEnabledAllowsExecution () {

    DefaultEnabledJob job = new DefaultEnabledJob();

    job.execute(null);

    Assert.assertTrue(job.proceedRan);
    Assert.assertNotNull(job.getStartTime());
    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
  }

  public void testLogOnZeroCountCompletesIdleSuccessRun () {

    HarnessJob job = new HarnessJob();

    // logOnZeroCount() == true forces the completion-log branch to fire even though the run was a
    // zero-count success, exercising the third disjunct of execute()'s logging decision.
    job.logZero = true;
    job.execute(null);

    Assert.assertEquals(job.getCount(), 0);
    Assert.assertEquals(job.getJobStatus(), SuccessOrFailure.SUCCESS);
    Assert.assertNotNull(job.getStopTime());
    Assert.assertTrue(job.cleanupRan);
  }

  /**
   * Subclass that does not override {@link QuartzProxyJob#isEnabled()}, so a firing exercises the
   * inherited default that always returns {@code true}.
   */
  private static class DefaultEnabledJob extends QuartzProxyJob {

    private boolean proceedRan = false;

    @Override
    public boolean logOnZeroCount () {

      return false;
    }

    @Override
    public void proceed () {

      proceedRan = true;
    }

    @Override
    public void cleanup () {
    }
  }
}

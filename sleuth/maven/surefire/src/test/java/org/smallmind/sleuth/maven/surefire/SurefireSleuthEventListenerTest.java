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
package org.smallmind.sleuth.maven.surefire;

import org.apache.maven.surefire.api.report.RunMode;
import org.smallmind.sleuth.runner.event.CancelledSleuthEvent;
import org.smallmind.sleuth.runner.event.ErrorSleuthEvent;
import org.smallmind.sleuth.runner.event.FailureSleuthEvent;
import org.smallmind.sleuth.runner.event.FatalSleuthEvent;
import org.smallmind.sleuth.runner.event.SkippedSleuthEvent;
import org.smallmind.sleuth.runner.event.SleuthEvent;
import org.smallmind.sleuth.runner.event.StartSleuthEvent;
import org.smallmind.sleuth.runner.event.SuccessSleuthEvent;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class SurefireSleuthEventListenerTest {

  private static final String CLASS_NAME = "org.example.SampleTest";
  private static final String METHOD_NAME = "sampleMethod";

  private static CapturingRunListener dispatch (SleuthEvent event) {

    CapturingRunListener captor = new CapturingRunListener();

    new SurefireSleuthEventListener(captor, RunMode.NORMAL_RUN).handle(event);

    return captor;
  }

  public void testStartMapsToTestStarting () {

    Assert.assertEquals(dispatch(new StartSleuthEvent(CLASS_NAME, METHOD_NAME)).getLastCall(), "testStarting");
  }

  public void testSuccessMapsToTestSucceeded () {

    Assert.assertEquals(dispatch(new SuccessSleuthEvent(CLASS_NAME, METHOD_NAME, 5L)).getLastCall(), "testSucceeded");
  }

  public void testCancelledMapsToTestSetCompleted () {

    Assert.assertEquals(dispatch(new CancelledSleuthEvent(CLASS_NAME, METHOD_NAME)).getLastCall(), "testSetCompleted");
  }

  public void testSkippedMapsToTestSkippedCarryingTheMessage () {

    CapturingRunListener captor = dispatch(new SkippedSleuthEvent(CLASS_NAME, METHOD_NAME, 0L, "skipped because"));

    Assert.assertEquals(captor.getLastCall(), "testSkipped");
    // The skip reason is passed as the report entry's nameText argument.
    Assert.assertEquals(captor.getLastReportEntry().getNameText(), "skipped because");
  }

  public void testFailureMapsToTestFailedWithStackTraceWriter () {

    AssertionError assertionError = new AssertionError("assertion failed");
    CapturingRunListener captor = dispatch(new FailureSleuthEvent(CLASS_NAME, METHOD_NAME, 5L, assertionError));

    Assert.assertEquals(captor.getLastCall(), "testFailed");
    assertStackTraceWriterCarries(captor, assertionError);
  }

  public void testErrorMapsToTestErrorWithStackTraceWriter () {

    IllegalStateException throwable = new IllegalStateException("unexpected");
    CapturingRunListener captor = dispatch(new ErrorSleuthEvent(CLASS_NAME, METHOD_NAME, 5L, throwable));

    Assert.assertEquals(captor.getLastCall(), "testError");
    assertStackTraceWriterCarries(captor, throwable);
  }

  public void testFatalCapturesThrowableAndReportsSkippedByUser () {

    IllegalStateException throwable = new IllegalStateException("fatal");
    CapturingRunListener captor = new CapturingRunListener();
    SurefireSleuthEventListener listener = new SurefireSleuthEventListener(captor, RunMode.NORMAL_RUN);

    listener.handle(new FatalSleuthEvent(CLASS_NAME, METHOD_NAME, 5L, throwable));

    Assert.assertTrue(captor.wasExecutionSkippedByUser());
    Assert.assertSame(listener.getThrowable(), throwable, "A fatal event's throwable must be retained for rethrow");
  }

  private static void assertStackTraceWriterCarries (CapturingRunListener captor, Throwable expected) {

    Assert.assertTrue(captor.getLastReportEntry().getStackTraceWriter() instanceof SleuthStackTraceWriter, "Failure/error/moot entries must carry a SleuthStackTraceWriter");

    SleuthStackTraceWriter writer = (SleuthStackTraceWriter)captor.getLastReportEntry().getStackTraceWriter();

    Assert.assertSame(writer.getThrowable().getTarget(), expected);
  }
}

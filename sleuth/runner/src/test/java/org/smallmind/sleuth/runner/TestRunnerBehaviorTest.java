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
package org.smallmind.sleuth.runner;

import org.smallmind.sleuth.runner.annotation.AfterTest;
import org.smallmind.sleuth.runner.annotation.BeforeTest;
import org.smallmind.sleuth.runner.event.SleuthEventType;
import org.testng.Assert;

// End-to-end coverage of TestRunner's outcome classification and the expected-exception state machine,
// driven through SleuthRunner with one worker thread for deterministic ordering. Fixtures are nested
// static classes using Sleuth's native annotations, named so the project's Surefire/TestNG scan does
// not collect them as real tests.
@org.testng.annotations.Test(groups = "unit")
public class TestRunnerBehaviorTest {

  private static CapturingSleuthEventListener run (boolean stopOnError, boolean stopOnFailure, Class<?> suiteClass) {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(new String[0], 1, stopOnError, stopOnFailure, suiteClass);

    return listener;
  }

  private static CapturingSleuthEventListener run (Class<?> suiteClass) {

    return run(false, false, suiteClass);
  }

  // Item 1: only non-Exception types are listed, so the expected-exception contract is IGNORED and a
  // thrown RuntimeException falls through to the normal ERROR classification.
  public void testNonExceptionExpectedTypesAreIgnored () {

    CapturingSleuthEventListener listener = run(IgnoredExpectedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "throwsWithOnlyErrorExpected"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "throwsWithOnlyErrorExpected"));
  }

  // Item 1: a real Exception type is expected but a different one is thrown -> UNEXPECTED -> FAILURE.
  public void testMismatchedExpectedExceptionIsFailure () {

    CapturingSleuthEventListener listener = run(UnexpectedExpectedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "throwsDifferentException"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.ERROR, "throwsDifferentException"));
  }

  // Item 1: an AssertionError is always a failure, even when an expected exception is declared.
  public void testAssertionErrorIsFailureEvenWithExpectedException () {

    CapturingSleuthEventListener listener = run(AssertionWithExpectedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "throwsAssertionDespiteExpected"));
  }

  // Item 1: the thrown type matches the second entry in the list -> EXPECTED -> SUCCESS.
  public void testLaterListedExpectedExceptionMatches () {

    CapturingSleuthEventListener listener = run(MultiExpectedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "throwsSecondListed"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.FAILURE, "throwsSecondListed"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.ERROR, "throwsSecondListed"));
  }

  // Item 2: a @BeforeTest failure produces a culprit, which skips the test body; the @AfterTest hook is
  // also skipped because AnnotationMethodology suppresses invocation whenever a culprit is present.
  public void testBeforeTestFailureSkipsTestAndAfterHook () {

    CapturingSleuthEventListener listener = run(BeforeTestThrowsSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "beforeThatThrows"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "theTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "afterHook"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "theTest"));
  }

  // Item 3: a failure with stopOnFailure cancels the run, so a lower-priority test queued behind it
  // never starts.
  public void testStopOnFailureCancelsRemainingTests () {

    CapturingSleuthEventListener listener = run(false, true, StopOnFailureSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "failFirst"));
    Assert.assertFalse(listener.hasMethod("secondAfterFailure"), "A test queued behind a stop-on-failure must not run");
  }

  // Item 3: an error with stopOnError cancels the run in the same way.
  public void testStopOnErrorCancelsRemainingTests () {

    CapturingSleuthEventListener listener = run(true, false, StopOnErrorSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "errorFirst"));
    Assert.assertFalse(listener.hasMethod("secondAfterError"), "A test queued behind a stop-on-error must not run");
  }

  // Reflective invocation that fails with something other than an InvocationTargetException (here a
  // private method yields an IllegalAccessException) is caught by the generic handler and reported as
  // an ERROR rather than a FAILURE.
  public void testInaccessibleTestMethodIsReportedAsError () {

    CapturingSleuthEventListener listener = run(InaccessibleMethodSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "privateTest"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.FAILURE, "privateTest"));
  }

  // A declared expected exception that never arrives is a FAILURE, and with stopOnFailure it cancels
  // the remaining tests — exercising the cancel branch on the missing-throw path specifically.
  public void testMissingExpectedExceptionWithStopOnFailureCancelsRemainingTests () {

    CapturingSleuthEventListener listener = run(false, true, ExpectedMissingStopSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "expectsButReturnsFirst"));
    Assert.assertFalse(listener.hasMethod("secondAfterMissing"), "A test queued behind the stop-on-failure must not run");
  }

  // The generic (non-InvocationTargetException) error path also honors stopOnError — exercises the
  // cancel branch of that catch, complementing the stopOnError=false case above.
  public void testInaccessibleTestMethodWithStopOnErrorCancels () {

    CapturingSleuthEventListener listener = run(true, false, InaccessibleMethodSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "privateTest"));
  }

  // A mismatched expected exception is an UNEXPECTED failure; with stopOnFailure it cancels — exercises
  // the cancel branch on the categorized-failure path (the stopOnFailure=false case is covered above).
  public void testMismatchedExpectedExceptionWithStopOnFailureCancels () {

    CapturingSleuthEventListener listener = run(false, true, UnexpectedExpectedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "throwsDifferentException"));
  }

  public static class IgnoredExpectedSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = OutOfMemoryError.class)
    public void throwsWithOnlyErrorExpected () {

      throw new IllegalStateException("not the declared Error type");
    }
  }

  public static class UnexpectedExpectedSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = IllegalStateException.class)
    public void throwsDifferentException () {

      throw new IllegalArgumentException("wrong exception");
    }
  }

  public static class AssertionWithExpectedSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = IllegalStateException.class)
    public void throwsAssertionDespiteExpected () {

      throw new AssertionError("assertion beats expectation");
    }
  }

  public static class MultiExpectedSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = {IllegalStateException.class, NumberFormatException.class})
    public void throwsSecondListed () {

      throw new NumberFormatException("the second listed type");
    }
  }

  public static class BeforeTestThrowsSuite {

    @BeforeTest
    public void beforeThatThrows () {

      throw new IllegalStateException("setup failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void theTest () {

    }

    @AfterTest
    public void afterHook () {

    }
  }

  public static class StopOnFailureSuite {

    @org.smallmind.sleuth.runner.annotation.Test(priority = 0)
    public void failFirst () {

      throw new AssertionError("fail and stop");
    }

    @org.smallmind.sleuth.runner.annotation.Test(priority = 1)
    public void secondAfterFailure () {

    }
  }

  public static class StopOnErrorSuite {

    @org.smallmind.sleuth.runner.annotation.Test(priority = 0)
    public void errorFirst () {

      throw new IllegalStateException("error and stop");
    }

    @org.smallmind.sleuth.runner.annotation.Test(priority = 1)
    public void secondAfterError () {

    }
  }

  public static class InaccessibleMethodSuite {

    // Discovered via getDeclaredMethods but not accessible to the runner's reflective invoke, so the
    // call throws IllegalAccessException rather than wrapping a thrown exception.
    @org.smallmind.sleuth.runner.annotation.Test
    private void privateTest () {

    }
  }

  public static class ExpectedMissingStopSuite {

    @org.smallmind.sleuth.runner.annotation.Test(priority = 0, expectedExceptions = IllegalStateException.class)
    public void expectsButReturnsFirst () {

    }

    @org.smallmind.sleuth.runner.annotation.Test(priority = 1)
    public void secondAfterMissing () {

    }
  }
}

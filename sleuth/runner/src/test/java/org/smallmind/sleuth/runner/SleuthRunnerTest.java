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

import org.smallmind.sleuth.runner.annotation.AfterSuite;
import org.smallmind.sleuth.runner.annotation.AfterTest;
import org.smallmind.sleuth.runner.annotation.BeforeSuite;
import org.smallmind.sleuth.runner.annotation.BeforeTest;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.smallmind.sleuth.runner.event.SleuthEventType;
import org.testng.Assert;

// End-to-end coverage of the runner: a single suite is executed with one worker thread (for
// deterministic ordering) and a CapturingSleuthEventListener records the emitted outcomes. The
// fixture suites use Sleuth's native annotations and are nested static classes named so the project's
// own Surefire/TestNG scan does not pick them up as real tests. The TestNG run marker is fully
// qualified because the fixtures import Sleuth's own @Test-family annotations.
@org.testng.annotations.Test(groups = "unit")
public class SleuthRunnerTest {

  private static CapturingSleuthEventListener run (String[] groups, boolean stopOnError, boolean stopOnFailure, Class<?> suiteClass) {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(groups, 1, stopOnError, stopOnFailure, suiteClass);

    return listener;
  }

  private static CapturingSleuthEventListener run (Class<?> suiteClass) {

    return run(new String[0], false, false, suiteClass);
  }

  public void testPassingTestAndLifecycleHooksEmitSuccess () {

    CapturingSleuthEventListener listener = run(SuccessSuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.FATAL), 0);
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "passes"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "beforeSuite"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "afterSuite"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "beforeTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "afterTest"));
    Assert.assertEquals(listener.countOfType(SleuthEventType.FAILURE), 0);
    Assert.assertEquals(listener.countOfType(SleuthEventType.ERROR), 0);
  }

  public void testAssertionErrorIsReportedAsFailure () {

    CapturingSleuthEventListener listener = run(FailureSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "failsAssertion"));
    Assert.assertEquals(listener.countOfType(SleuthEventType.ERROR), 0, "An AssertionError is a failure, not an error");
  }

  public void testUncheckedExceptionIsReportedAsError () {

    CapturingSleuthEventListener listener = run(ErrorSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "throwsRuntime"));
    Assert.assertEquals(listener.countOfType(SleuthEventType.FAILURE), 0);
  }

  public void testMatchingExpectedExceptionIsSuccess () {

    CapturingSleuthEventListener listener = run(ExpectedExceptionSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "throwsExpected"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.ERROR, "throwsExpected"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.FAILURE, "throwsExpected"));
  }

  public void testMissingExpectedExceptionIsFailure () {

    CapturingSleuthEventListener listener = run(MissingExpectedExceptionSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.FAILURE, "expectsButReturns"));
  }

  public void testDependentTestIsSkippedWhenPrerequisiteFails () {

    CapturingSleuthEventListener listener = run(DependencySuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "failingPrereq"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "needsPrereq"), "A hard dependency on a failed test must be skipped");
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "needsPrereq"));
  }

  public void testDisabledTestIsNotExecuted () {

    CapturingSleuthEventListener listener = run(DisabledSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "enabledOne"));
    Assert.assertFalse(listener.hasMethod("disabledOne"), "A disabled test must not produce any event");
  }

  public void testGroupFilterExcludesNonMatchingSuite () {

    CapturingSleuthEventListener listener = run(new String[] {"beta"}, false, false, GroupedSuite.class);

    Assert.assertFalse(listener.hasMethod("groupedTest"), "A suite outside the requested groups must not run");
    Assert.assertEquals(listener.countOfType(SleuthEventType.SUCCESS), 0);
  }

  public void testGroupFilterIncludesMatchingSuite () {

    CapturingSleuthEventListener listener = run(new String[] {"alpha"}, false, false, GroupedSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "groupedTest"));
  }

  public static class SuccessSuite {

    @BeforeSuite
    public void beforeSuite () {

    }

    @AfterSuite
    public void afterSuite () {

    }

    @BeforeTest
    public void beforeTest () {

    }

    @AfterTest
    public void afterTest () {

    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void passes () {

    }
  }

  public static class FailureSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void failsAssertion () {

      throw new AssertionError("expected this");
    }
  }

  public static class ErrorSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void throwsRuntime () {

      throw new IllegalStateException("unexpected");
    }
  }

  public static class ExpectedExceptionSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = IllegalStateException.class)
    public void throwsExpected () {

      throw new IllegalStateException("as promised");
    }
  }

  public static class MissingExpectedExceptionSuite {

    @org.smallmind.sleuth.runner.annotation.Test(expectedExceptions = IllegalStateException.class)
    public void expectsButReturns () {

    }
  }

  public static class DependencySuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void failingPrereq () {

      throw new IllegalStateException("prerequisite failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test(dependsOn = {"failingPrereq"})
    public void needsPrereq () {

    }
  }

  public static class DisabledSuite {

    @org.smallmind.sleuth.runner.annotation.Test(enabled = false)
    public void disabledOne () {

    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void enabledOne () {

    }
  }

  @Suite(groups = {"alpha"})
  public static class GroupedSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void groupedTest () {

    }
  }
}

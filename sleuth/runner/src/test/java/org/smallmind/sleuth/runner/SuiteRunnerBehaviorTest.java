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
import org.smallmind.sleuth.runner.annotation.BeforeSuite;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.smallmind.sleuth.runner.event.SleuthEventType;
import org.testng.Assert;

// End-to-end coverage of SuiteRunner's failure paths: instantiation problems surface as FatalSleuthEvent,
// and a before-suite failure cascades into skipped tests and skipped after-suite hooks unless those
// hooks are marked alwaysRun (which an inherited dependsOn culprit still overrides). Fixtures are
// nested static classes using Sleuth's native annotations and are named so the project's Surefire scan
// ignores them.
@org.testng.annotations.Test(groups = "unit")
public class SuiteRunnerBehaviorTest {

  private static CapturingSleuthEventListener run (Class<?> suiteClass) {

    return run(false, false, suiteClass);
  }

  private static CapturingSleuthEventListener run (boolean stopOnError, boolean stopOnFailure, Class<?>... classes) {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(new String[0], 1, stopOnError, stopOnFailure, classes);

    return listener;
  }

  // Item 4: no accessible no-arg constructor -> TestProcessingException -> FatalSleuthEvent.
  public void testSuiteWithoutNoArgConstructorIsFatal () {

    CapturingSleuthEventListener listener = run(NoDefaultConstructorSuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.FATAL), 1);
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "neverRuns"));
  }

  // Item 4: a constructor that throws is wrapped and surfaces as a FatalSleuthEvent as well.
  public void testSuiteWithThrowingConstructorIsFatal () {

    CapturingSleuthEventListener listener = run(ThrowingConstructorSuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.FATAL), 1);
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "neverRuns"));
  }

  // Item 5: a @BeforeSuite failure produces a suite-level culprit that skips every test and the
  // @AfterSuite hook (which is not marked alwaysRun, so AnnotationMethodology suppresses it).
  public void testBeforeSuiteFailureSkipsTestsAndAfterSuite () {

    CapturingSleuthEventListener listener = run(BeforeSuiteThrowsSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "suiteSetup"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "suiteTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "suiteTeardown"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "suiteTest"));
  }

  // A @BeforeSuite failure still runs an @AfterSuite(alwaysRun = true) hook, because the culprit arose
  // inside this suite; a sibling hook without alwaysRun remains skipped.
  public void testBeforeSuiteFailureRunsAlwaysRunAfterSuite () {

    CapturingSleuthEventListener listener = run(BeforeSuiteThrowsAlwaysRunSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "alwaysRunSuiteSetup"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "alwaysRunSuiteTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "alwaysRunTeardown"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "plainTeardown"));
  }

  // A culprit inherited from a failed dependsOn prerequisite means nothing in this suite ran, so even an
  // alwaysRun teardown is skipped.
  public void testInheritedCulpritSkipsAlwaysRunAfterSuite () {

    CapturingSleuthEventListener listener = run(false, false, PrerequisiteThrowsSuite.class, DependentAlwaysRunSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "prerequisiteSetup"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "dependentTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "dependentTeardown"));
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "dependentTeardown"), "An inherited culprit must skip even alwaysRun teardown");
  }

  // The first suite (priority 0) fails to instantiate; with stopOnError the run is cancelled, so the
  // priority-1 suite gated behind it never starts — exercising the stop-on-error branch of the
  // suite-level catch.
  public void testSuiteInstantiationFailureWithStopOnErrorCancelsLaterSuites () {

    CapturingSleuthEventListener listener = run(true, false, FailingFirstSuite.class, NormalSecondSuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.FATAL), 1);
    Assert.assertFalse(listener.hasMethod("secondSuiteTest"), "stopOnError on a suite failure must cancel later suites");
  }

  public static class NoDefaultConstructorSuite {

    public NoDefaultConstructorSuite (String required) {

    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void neverRuns () {

    }
  }

  public static class ThrowingConstructorSuite {

    public ThrowingConstructorSuite () {

      throw new IllegalStateException("constructor boom");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void neverRuns () {

    }
  }

  public static class BeforeSuiteThrowsSuite {

    @BeforeSuite
    public void suiteSetup () {

      throw new IllegalStateException("suite setup failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void suiteTest () {

    }

    @AfterSuite
    public void suiteTeardown () {

    }
  }

  public static class BeforeSuiteThrowsAlwaysRunSuite {

    @BeforeSuite
    public void alwaysRunSuiteSetup () {

      throw new IllegalStateException("suite setup failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void alwaysRunSuiteTest () {

    }

    @AfterSuite(alwaysRun = true)
    public void alwaysRunTeardown () {

    }

    @AfterSuite
    public void plainTeardown () {

    }
  }

  @Suite(groups = {})
  public static class PrerequisiteThrowsSuite {

    @BeforeSuite
    public void prerequisiteSetup () {

      throw new IllegalStateException("prerequisite setup failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void prerequisiteTest () {

    }
  }

  @Suite(groups = {}, dependsOn = {"org.smallmind.sleuth.runner.SuiteRunnerBehaviorTest$PrerequisiteThrowsSuite"})
  public static class DependentAlwaysRunSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void dependentTest () {

    }

    @AfterSuite(alwaysRun = true)
    public void dependentTeardown () {

    }
  }

  @Suite(groups = {}, priority = 0)
  public static class FailingFirstSuite {

    public FailingFirstSuite (String required) {

    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void neverRunsHere () {

    }
  }

  @Suite(groups = {}, priority = 1)
  public static class NormalSecondSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void secondSuiteTest () {

    }
  }
}

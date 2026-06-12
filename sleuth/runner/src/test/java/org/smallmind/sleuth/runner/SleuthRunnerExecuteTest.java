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

import java.util.Collections;
import org.smallmind.sleuth.runner.annotation.BeforeSuite;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.smallmind.sleuth.runner.event.SleuthEventType;
import org.testng.Assert;

// End-to-end coverage of SleuthRunner.execute: cross-suite scheduling (priority tiers and suite-level
// dependsOn skip propagation), cancellation, the group-filter matrix, and the no-op guards. One worker
// thread keeps ordering deterministic. Fixtures are nested static classes using Sleuth's native
// annotations and are named so the project's Surefire scan ignores them.
@org.testng.annotations.Test(groups = "unit")
public class SleuthRunnerExecuteTest {

  // Binary name of the nested AlphaSuite, used as a suite-level dependsOn target. Verified against the
  // live class in the dependency test so a rename fails loudly rather than silently breaking the edge.
  private static final String ALPHA_SUITE_NAME = "org.smallmind.sleuth.runner.SleuthRunnerExecuteTest$AlphaSuite";

  private static CapturingSleuthEventListener run (String[] groups, Class<?>... classes) {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(groups, 1, false, false, classes);

    return listener;
  }

  // Item 6: a lower-priority suite is gated behind a higher-priority suite, so the high-priority test
  // completes before the low-priority test starts.
  public void testSuitePriorityOrdersAcrossSuites () {

    CapturingSleuthEventListener listener = run(new String[0], LowPrioritySuite.class, HighPrioritySuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "highPriorityTest"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "lowPriorityTest"));
    Assert.assertTrue(listener.indexOf(SleuthEventType.SUCCESS, "highPriorityTest") < listener.indexOf(SleuthEventType.START, "lowPriorityTest"), "the priority-0 suite must finish before the priority-1 suite starts");
  }

  // Item 6: a suite whose @BeforeSuite fails records a culprit; a second suite that hard-depends on it
  // is skipped with that culprit propagated.
  public void testSuiteDependsOnSkipsDownstreamSuite () {

    Assert.assertEquals(AlphaSuite.class.getName(), ALPHA_SUITE_NAME, "dependsOn target name is out of date");

    CapturingSleuthEventListener listener = run(new String[0], AlphaSuite.class, BetaSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.ERROR, "alphaSetup"));
    Assert.assertTrue(listener.hasEvent(SleuthEventType.SKIPPED, "betaTest"), "a suite depending on a failed suite must be skipped");
    Assert.assertFalse(listener.hasEvent(SleuthEventType.SUCCESS, "betaTest"));
  }

  // Item 6: cancelling before execution means no suite is dispatched and a CancelledSleuthEvent fires.
  public void testCancelBeforeExecuteEmitsCancelledAndRunsNothing () {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.cancel();
    sleuthRunner.execute(new String[0], 1, false, false, PlainSuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.CANCELLED), 1);
    Assert.assertFalse(listener.hasMethod("plainTest"));
  }

  // Item 7: a default suite (no @Suite, hence no groups) is excluded when a non-empty group filter is active.
  public void testDefaultSuiteExcludedByActiveGroupFilter () {

    CapturingSleuthEventListener listener = run(new String[] {"beta"}, PlainSuite.class);

    Assert.assertFalse(listener.hasMethod("plainTest"));
    Assert.assertEquals(listener.countOfType(SleuthEventType.SUCCESS), 0);
  }

  // Item 7: a null group filter bypasses filtering entirely and runs every suite.
  public void testNullGroupFilterRunsEverything () {

    CapturingSleuthEventListener listener = run(null, PlainSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "plainTest"));
  }

  // Item 7: a suite is included when any one of its groups intersects the requested set.
  public void testMatchingOneOfSeveralGroups () {

    CapturingSleuthEventListener listener = run(new String[] {"zulu", "yankee"}, MultiGroupSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "multiGroupTest"));
  }

  // Item 7: a disabled suite is excluded regardless of group filtering.
  public void testDisabledSuiteIsExcluded () {

    CapturingSleuthEventListener listener = run(new String[0], DisabledSuite.class);

    Assert.assertFalse(listener.hasMethod("disabledSuiteTest"));
  }

  // A suite-level dependsOn naming a node that is never discovered makes the topological sort throw,
  // which execute() catches and reports as a single FatalSleuthEvent before anything runs.
  public void testMissingSuiteDependencyIsFatal () {

    CapturingSleuthEventListener listener = run(new String[0], MissingDependencySuite.class);

    Assert.assertEquals(listener.countOfType(SleuthEventType.FATAL), 1);
    Assert.assertFalse(listener.hasMethod("orphanTest"), "Nothing runs when the suite graph cannot be resolved");
  }

  // A class carrying no recognized annotations resolves to a null dictionary and is silently skipped,
  // while a real suite in the same batch still runs.
  public void testUnrecognizedClassIsSkipped () {

    CapturingSleuthEventListener listener = run(new String[0], NotASuite.class, PlainSuite.class);

    Assert.assertTrue(listener.hasEvent(SleuthEventType.SUCCESS, "plainTest"));
    Assert.assertFalse(listener.hasMethod("notATestMethod"), "A class with no Sleuth or TestNG annotations contributes nothing");
  }

  // Item 8: a null class iterable is a no-op that fires no events.
  public void testNullClassIterableIsNoOp () {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(new String[0], 1, false, false, (Iterable<Class<?>>)null);

    Assert.assertTrue(listener.getEvents().isEmpty());
  }

  // Item 8: an empty (non-null) class iterable simply produces no work.
  public void testEmptyClassIterableRunsNothing () {

    SleuthRunner sleuthRunner = new SleuthRunner();
    CapturingSleuthEventListener listener = new CapturingSleuthEventListener();

    sleuthRunner.addListener(listener);
    sleuthRunner.execute(new String[0], 1, false, false, Collections.<Class<?>>emptyList());

    Assert.assertTrue(listener.getEvents().isEmpty());
  }

  @Suite(groups = {}, priority = 0)
  public static class HighPrioritySuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void highPriorityTest () {

    }
  }

  @Suite(groups = {}, priority = 1)
  public static class LowPrioritySuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void lowPriorityTest () {

    }
  }

  public static class AlphaSuite {

    @BeforeSuite
    public void alphaSetup () {

      throw new IllegalStateException("alpha suite setup failed");
    }

    @org.smallmind.sleuth.runner.annotation.Test
    public void alphaTest () {

    }
  }

  @Suite(groups = {}, dependsOn = {ALPHA_SUITE_NAME})
  public static class BetaSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void betaTest () {

    }
  }

  public static class PlainSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void plainTest () {

    }
  }

  @Suite(groups = {"xray", "yankee"})
  public static class MultiGroupSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void multiGroupTest () {

    }
  }

  @Suite(groups = {}, enabled = false)
  public static class DisabledSuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void disabledSuiteTest () {

    }
  }

  @Suite(groups = {}, dependsOn = {"com.example.DoesNotExist"})
  public static class MissingDependencySuite {

    @org.smallmind.sleuth.runner.annotation.Test
    public void orphanTest () {

    }
  }

  public static class NotASuite {

    public void notATestMethod () {

    }
  }
}

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

import java.util.ArrayList;
import java.util.List;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DependencyAnalysisTest {

  private static Dependency<Suite, String> node (String name, int priority, String[] executeAfter, String[] dependsOn) {

    return new Dependency<>(name, null, name, priority, executeAfter, dependsOn, null);
  }

  // Drains the queue in prerequisite-satisfied lock-step (completing each node before polling the
  // next), returning the order in which nodes were dispatched. Because the analysis produces a
  // topologically sorted, gated queue, at least one node is always eligible until the queue empties,
  // so this never blocks.
  private static List<String> drain (DependencyQueue<Suite, String> queue) {

    List<String> order = new ArrayList<>();
    Dependency<Suite, String> dependency;

    while ((dependency = queue.poll()) != null) {
      order.add(dependency.getName());
      queue.complete(dependency);
    }

    return order;
  }

  public void testHardDependencyOrdersPrerequisiteFirst () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    analysis.add(node("alpha", 0, null, null));
    analysis.add(node("beta", 0, null, new String[] {"alpha"}));

    DependencyQueue<Suite, String> queue = analysis.calculate();

    Assert.assertEquals(queue.size(), 2);

    List<String> order = drain(queue);

    Assert.assertTrue(order.indexOf("alpha") < order.indexOf("beta"), "alpha must precede beta, was " + order);
  }

  public void testExecuteAfterOrdersSoftPredecessorFirst () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    analysis.add(node("first", 0, null, null));
    analysis.add(node("second", 0, new String[] {"first"}, null));

    List<String> order = drain(analysis.calculate());

    Assert.assertTrue(order.indexOf("first") < order.indexOf("second"), "first must precede second, was " + order);
  }

  public void testLowerPriorityTierCompletesBeforeHigher () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    analysis.add(node("late", 5, null, null));
    analysis.add(node("early", 1, null, null));

    List<String> order = drain(analysis.calculate());

    Assert.assertTrue(order.indexOf("early") < order.indexOf("late"), "priority 1 must precede priority 5, was " + order);
  }

  public void testForwardReferencedDependencyIsAlignedNotMissing () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    // "child" is added first and names a prerequisite that has not been registered yet; a placeholder
    // is created. Adding the real "parent" afterward must align that placeholder rather than leaving
    // an incomplete (missing) node.
    analysis.add(node("child", 0, null, new String[] {"parent"}));
    analysis.add(node("parent", 0, null, null));

    DependencyQueue<Suite, String> queue = analysis.calculate();

    Assert.assertEquals(queue.size(), 2);

    List<String> order = drain(queue);

    Assert.assertTrue(order.indexOf("parent") < order.indexOf("child"), "parent must precede child, was " + order);
  }

  public void testCyclicDependencyThrows () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    analysis.add(node("one", 0, null, new String[] {"two"}));
    analysis.add(node("two", 0, null, new String[] {"one"}));

    try {
      analysis.calculate();
      Assert.fail("Expected a TestDependencyException for the cycle");
    } catch (TestDependencyException testDependencyException) {
      Assert.assertTrue(testDependencyException.getMessage().contains("Cyclic"), "Message should describe a cycle, was: " + testDependencyException.getMessage());
    }
  }

  public void testMissingDependencyThrows () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    // "ghost" is never registered as a real node, so its placeholder stays incomplete.
    analysis.add(node("solo", 0, null, new String[] {"ghost"}));

    try {
      analysis.calculate();
      Assert.fail("Expected a TestDependencyException for the missing node");
    } catch (TestDependencyException testDependencyException) {
      Assert.assertTrue(testDependencyException.getMessage().contains("Missing"), "Message should describe a missing dependency, was: " + testDependencyException.getMessage());
    }
  }

  public void testEmptyAnalysisProducesEmptyQueue () {

    DependencyAnalysis<Suite, String> analysis = new DependencyAnalysis<>(Suite.class);

    DependencyQueue<Suite, String> queue = analysis.calculate();

    Assert.assertEquals(queue.size(), 0);
    Assert.assertNull(queue.poll());
  }

  public void testDependencyEqualityIsByName () {

    Dependency<Suite, String> first = node("same", 0, null, null);
    Dependency<Suite, String> second = new Dependency<>("same");

    Assert.assertEquals(first, second);
    Assert.assertEquals(first.hashCode(), second.hashCode());
    Assert.assertNotEquals(first, node("different", 0, null, null));
  }

  public void testDependencyEqualityWithNullNames () {

    Dependency<Suite, String> nullName = new Dependency<>(null);
    Dependency<Suite, String> alsoNullName = new Dependency<>(null);
    Dependency<Suite, String> named = node("named", 0, null, null);

    // Both names null -> equal; one null and one named -> not equal in either direction.
    Assert.assertEquals(nullName, alsoNullName);
    Assert.assertNotEquals(nullName, named);
    Assert.assertNotEquals(named, nullName);
    Assert.assertFalse(named.equals(null));
    Assert.assertFalse(named.equals("not a dependency"));
  }
}

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

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.sleuth.runner.annotation.Suite;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class DependencyQueueTest {

  private static Dependency<Suite, String> node (String name, String[] executeAfter, String[] dependsOn) {

    return new Dependency<>(name, null, name, 0, executeAfter, dependsOn, null);
  }

  public void testSizeReflectsSeededList () {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    list.add(node("a", null, null));
    list.add(node("b", null, null));

    Assert.assertEquals(new DependencyQueue<>(list).size(), 2);
  }

  public void testPrerequisiteFreeNodesDrainThenNull () {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    list.add(node("a", null, null));
    list.add(node("b", null, null));

    DependencyQueue<Suite, String> queue = new DependencyQueue<>(list);

    Assert.assertNotNull(queue.poll());
    Assert.assertNotNull(queue.poll());
    Assert.assertNull(queue.poll());
  }

  public void testIneligibleNodeIsSkippedUntilPrerequisiteCompletes () {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    // The blocked node is placed first to prove poll() scans past it to the eligible prerequisite
    // rather than returning the first list entry blindly.
    Dependency<Suite, String> blocked = node("blocked", new String[] {"ready"}, null);
    Dependency<Suite, String> ready = node("ready", null, null);

    list.add(blocked);
    list.add(ready);

    DependencyQueue<Suite, String> queue = new DependencyQueue<>(list);

    Dependency<Suite, String> firstOut = queue.poll();

    Assert.assertEquals(firstOut.getName(), "ready");
    queue.complete(firstOut);

    Dependency<Suite, String> secondOut = queue.poll();

    Assert.assertEquals(secondOut.getName(), "blocked");
  }

  public void testCulpritPropagatesThroughHardDependency () {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    Dependency<Suite, String> prerequisite = node("prereq", null, null);
    Dependency<Suite, String> dependent = node("dependent", null, new String[] {"prereq"});
    Culprit culprit = new Culprit("org.example.Thing", "boom", new RuntimeException("boom"));

    prerequisite.setCulprit(culprit);
    list.add(prerequisite);
    list.add(dependent);

    DependencyQueue<Suite, String> queue = new DependencyQueue<>(list);

    queue.complete(queue.poll());

    Dependency<Suite, String> dispatched = queue.poll();

    Assert.assertEquals(dispatched.getName(), "dependent");
    Assert.assertSame(dispatched.getCulprit(), culprit, "The failed prerequisite's culprit must propagate to the dependent");
  }

  public void testSoftPredecessorDoesNotPropagateCulprit () {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    Dependency<Suite, String> predecessor = node("predecessor", null, null);
    Dependency<Suite, String> follower = node("follower", new String[] {"predecessor"}, null);

    predecessor.setCulprit(new Culprit("org.example.Thing", "boom", new RuntimeException("boom")));
    list.add(predecessor);
    list.add(follower);

    DependencyQueue<Suite, String> queue = new DependencyQueue<>(list);

    queue.complete(queue.poll());

    Dependency<Suite, String> dispatched = queue.poll();

    Assert.assertEquals(dispatched.getName(), "follower");
    Assert.assertNull(dispatched.getCulprit(), "executeAfter is a soft ordering and must not inherit a culprit");
  }

  public void testPollInterruptedWhileBlockedWrapsAsRuntimeException ()
    throws InterruptedException {

    LinkedList<Dependency<Suite, String>> list = new LinkedList<>();

    // The single node depends on a prerequisite that never completes, so poll() blocks in wait().
    list.add(node("foreverBlocked", null, new String[] {"neverCompletes"}));

    DependencyQueue<Suite, String> queue = new DependencyQueue<>(list);
    AtomicReference<Throwable> caught = new AtomicReference<>();
    Thread poller = new Thread(() -> {
      try {
        queue.poll();
      } catch (Throwable throwable) {
        caught.set(throwable);
      }
    });

    poller.start();
    Thread.sleep(250);
    poller.interrupt();
    poller.join(2000);

    Assert.assertNotNull(caught.get(), "Interrupting a blocked poll() must surface a throwable");
    Assert.assertTrue(caught.get() instanceof RuntimeException, "Interruption is wrapped as a RuntimeException");
    Assert.assertTrue(caught.get().getCause() instanceof InterruptedException, "The wrapped cause is the InterruptedException");
  }
}

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
package org.smallmind.nutsnbolts.scope;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class ScopeFactoryTest {

  public void testScopeIsVisibleOnlyInsideTheOperation () {

    Assert.assertNull(ScopeFactory.getScope(TestScope.class));

    String seen = ScopeFactory.callInScope(new TestScope("a"), () -> ScopeFactory.getScope(TestScope.class).getValue());

    Assert.assertEquals(seen, "a");
    Assert.assertNull(ScopeFactory.getScope(TestScope.class));
  }

  public void testNestedScopeOfSameClassShadowsThenRestoresOuter () {

    ScopeFactory.runInScope(new TestScope("outer"), () -> {
      Assert.assertEquals(ScopeFactory.getScope(TestScope.class).getValue(), "outer");

      ScopeFactory.runInScope(new TestScope("inner"), () -> Assert.assertEquals(ScopeFactory.getScope(TestScope.class).getValue(), "inner"));

      Assert.assertEquals(ScopeFactory.getScope(TestScope.class).getValue(), "outer");
    });
  }

  public void testNestedScopeOfDifferentClassKeepsOuterVisible () {

    ScopeFactory.runInScope(new TestScope("outer"), () -> ScopeFactory.runInScope(new OtherScope(), () -> {
      Assert.assertEquals(ScopeFactory.getScope(TestScope.class).getValue(), "outer");
      Assert.assertNotNull(ScopeFactory.getScope(OtherScope.class));
    }));

    Assert.assertNull(ScopeFactory.getScope(OtherScope.class));
  }

  public void testScopeIsUnboundAfterTheOperationThrows () {

    try {
      ScopeFactory.callInScope(new TestScope("failing"), () -> {
        throw new IOException("expected");
      });
      Assert.fail("The operation should have thrown");
    } catch (IOException ioException) {
      Assert.assertEquals(ioException.getMessage(), "expected");
    }

    Assert.assertNull(ScopeFactory.getScope(TestScope.class));
  }

  public void testCallInScopesBindsEveryScopeWithLaterWinningOnSharedClass () {

    Scope[] scopes = new Scope[] {new TestScope("first"), new OtherScope(), new TestScope("second")};

    ScopeFactory.runInScopes(scopes, () -> {
      Assert.assertEquals(ScopeFactory.getScope(TestScope.class).getValue(), "second");
      Assert.assertSame(ScopeFactory.getScope(OtherScope.class), scopes[1]);
    });
  }

  public void testCallInScopesWithNullOrEmptyArrayRunsUnbound () {

    Assert.assertEquals(ScopeFactory.callInScopes(null, () -> "ran"), "ran");
    Assert.assertEquals(ScopeFactory.callInScopes(new Scope[0], () -> "ran"), "ran");
    Assert.assertNull(ScopeFactory.getScope(TestScope.class));
  }

  @Test(expectedExceptions = ScopeException.class)
  public void testCallInScopeRejectsNullScope () {

    ScopeFactory.callInScope(null, () -> null);
  }

  @Test(expectedExceptions = ScopeException.class)
  public void testCallInScopesRejectsNullElement () {

    ScopeFactory.runInScopes(new Scope[] {new TestScope("present"), null}, () -> Assert.fail("The operation should not run"));
  }

  public void testLifecycleCallbacksBracketTheOperationInOrder () {

    LinkedList<String> events = new LinkedList<>();
    Lifecycle alpha = new Lifecycle("alpha", events, false);
    Lifecycle beta = new Lifecycle("beta", events, false);

    ScopeFactory.runInScopes(new Scope[] {alpha, beta}, () -> {
      Assert.assertSame(ScopeFactory.getScope(Lifecycle.class), beta);
      events.add("body");
    });

    Assert.assertEquals(events, List.of("enter:alpha", "enter:beta", "body", "exit:beta", "exit:alpha"));
  }

  public void testLifecycleAfterExitRunsWhenTheOperationThrows () {

    LinkedList<String> events = new LinkedList<>();

    try {
      ScopeFactory.runInScope(new Lifecycle("solo", events, false), () -> {
        throw new IllegalStateException("expected");
      });
      Assert.fail("The operation should have thrown");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertEquals(illegalStateException.getMessage(), "expected");
    }

    Assert.assertEquals(events, List.of("enter:solo", "exit:solo"));
  }

  public void testLifecycleBeforeEnterFailureExitsOnlyEnteredScopes () {

    LinkedList<String> events = new LinkedList<>();

    try {
      ScopeFactory.runInScopes(new Scope[] {new Lifecycle("ok", events, false), new Lifecycle("bad", events, true)}, () -> Assert.fail("The operation should not run"));
      Assert.fail("The failing beforeEnter should have propagated");
    } catch (IllegalStateException illegalStateException) {
      Assert.assertEquals(illegalStateException.getMessage(), "enter failed:bad");
    }

    Assert.assertEquals(events, List.of("enter:ok", "exit:ok"));
    Assert.assertNull(ScopeFactory.getScope(Lifecycle.class));
  }

  @ExpectedScopes({TestScope.class})
  public void annotatedTarget () {

  }

  public void unannotatedTarget () {

  }

  public void testFilterScopesOnAcceptsWhenExpectationIsSatisfied ()
    throws NoSuchMethodException {

    Method method = ScopeFactoryTest.class.getMethod("annotatedTarget");

    ScopeFactory.runInScope(new TestScope("present"), () -> {

      Scope[] scopes = ScopeFactory.filterScopesOn(method, Scope.class);

      Assert.assertEquals(scopes.length, 1);
      Assert.assertTrue(scopes[0] instanceof TestScope);
    });
  }

  public void testFilterScopesOnIncludesOnlyAssignableScopes ()
    throws NoSuchMethodException {

    Method method = ScopeFactoryTest.class.getMethod("unannotatedTarget");

    ScopeFactory.runInScopes(new Scope[] {new TestScope("present"), new OtherScope()}, () -> {

      Scope[] scopes = ScopeFactory.filterScopesOn(method, OtherScope.class);

      Assert.assertEquals(scopes.length, 1);
      Assert.assertTrue(scopes[0] instanceof OtherScope);
    });
  }

  @Test(expectedExceptions = ScopeException.class)
  public void testFilterScopesOnRejectsWhenExpectationIsMissing ()
    throws NoSuchMethodException {

    Method method = ScopeFactoryTest.class.getMethod("annotatedTarget");

    ScopeFactory.filterScopesOn(method, Scope.class);
  }

  public void testFilterScopesOnReturnsEmptyArrayWhenNothingIsBound ()
    throws NoSuchMethodException {

    Method method = ScopeFactoryTest.class.getMethod("unannotatedTarget");

    Assert.assertEquals(ScopeFactory.filterScopesOn(method, Scope.class).length, 0);
  }

  public void testPlainChildThreadDoesNotInheritBindings () {

    AtomicReference<TestScope> seen = new AtomicReference<>(new TestScope("sentinel"));

    ScopeFactory.runInScope(new TestScope("parent"), () -> joinChild(() -> seen.set(ScopeFactory.getScope(TestScope.class))));

    Assert.assertNull(seen.get());
  }

  public void testSnapshotRebindsCapturedScopesOnAnotherThread () {

    LinkedList<String> events = new LinkedList<>();
    AtomicReference<String> seen = new AtomicReference<>();
    AtomicReference<Scope> otherSeen = new AtomicReference<>(new OtherScope());

    ScopeFactory.runInScope(new Lifecycle("lifecycle", events, false), () -> ScopeFactory.runInScope(new TestScope("captured"), () -> {

      ScopeSnapshot snapshot = ScopeFactory.snapshot();

      joinChild(() -> snapshot.run(() -> {
        seen.set(ScopeFactory.getScope(TestScope.class).getValue());
        otherSeen.set(ScopeFactory.getScope(OtherScope.class));
      }));
    }));

    Assert.assertEquals(seen.get(), "captured");
    Assert.assertNull(otherSeen.get());
    Assert.assertEquals(events, List.of("enter:lifecycle", "exit:lifecycle"));
  }

  public void testSnapshotIsUnaffectedByLaterEntriesAndIsReusable () {

    ScopeSnapshot emptySnapshot = ScopeFactory.snapshot();
    ScopeSnapshot boundSnapshot = ScopeFactory.callInScope(new TestScope("captured"), ScopeFactory::snapshot);

    Assert.assertNull(emptySnapshot.call(() -> ScopeFactory.getScope(TestScope.class)));
    Assert.assertEquals(boundSnapshot.call(() -> ScopeFactory.getScope(TestScope.class).getValue()), "captured");
    Assert.assertEquals(boundSnapshot.call(() -> ScopeFactory.getScope(TestScope.class).getValue()), "captured");
    Assert.assertNull(ScopeFactory.getScope(TestScope.class));
  }

  private static void joinChild (Runnable runnable) {

    Thread child = new Thread(runnable);

    child.setName("scope-factory-test-child");
    child.setDaemon(true);
    child.start();
    try {
      child.join();
    } catch (InterruptedException interruptedException) {
      throw new IllegalStateException(interruptedException);
    }
  }

  public static class TestScope implements Scope {

    private final String value;

    public TestScope (String value) {

      this.value = value;
    }

    public String getValue () {

      return value;
    }
  }

  public static class OtherScope implements Scope {

  }

  public static class Lifecycle implements Scope, LifecycleAware {

    private final LinkedList<String> events;
    private final String name;
    private final boolean failOnEnter;

    public Lifecycle (String name, LinkedList<String> events, boolean failOnEnter) {

      this.name = name;
      this.events = events;
      this.failOnEnter = failOnEnter;
    }

    @Override
    public void beforeEnter () {

      if (failOnEnter) {
        throw new IllegalStateException("enter failed:" + name);
      }

      events.add("enter:" + name);
    }

    @Override
    public void afterExit () {

      events.add("exit:" + name);
    }
  }
}

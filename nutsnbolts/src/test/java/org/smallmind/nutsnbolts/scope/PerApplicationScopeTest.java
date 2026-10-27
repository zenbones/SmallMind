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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class PerApplicationScopeTest {

  private static void joinChild (Runnable runnable) {

    Thread child = new Thread(runnable);

    child.setName("per-application-scope-test-child");
    child.setDaemon(true);
    joinThread(child);
  }

  private static void joinThread (Thread thread) {

    thread.start();
    try {
      thread.join();
    } catch (InterruptedException interruptedException) {
      throw new IllegalStateException(interruptedException);
    }
  }

  private static Thread namedThread (Runnable runnable) {

    Thread thread = new Thread(runnable);

    thread.setName("per-application-scope-test-factory");
    thread.setDaemon(true);

    return thread;
  }

  public void testDataIsVisibleInsideRun () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "value");
      Assert.assertEquals(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class), "value");
    });
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testSetWithoutScopeThrows () {

    PerApplicationScope.setPerApplicationData(SampleManager.class, "value");
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testGetWithoutScopeThrows () {

    PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
  }

  public void testBindingEndsWhenRunReturns () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "value"));

    try {
      PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      Assert.fail("The binding should not survive the run");
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
      // expected
    }
  }

  public void testDataPersistsAcrossRunsOfTheSameScope () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "value"));

    Assert.assertEquals(scope.call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)), "value");
  }

  public void testSeparateScopesIsolateData () {

    PerApplicationScope first = new PerApplicationScope();
    PerApplicationScope second = new PerApplicationScope();

    first.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "first"));

    Assert.assertNull(second.call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));
    Assert.assertEquals(first.call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)), "first");
  }

  public void testNestedRunShadowsThenRestoresOuterScope () {

    PerApplicationScope outer = new PerApplicationScope();
    PerApplicationScope inner = new PerApplicationScope();

    outer.run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "outer");

      inner.run(() -> {
        Assert.assertNull(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
        PerApplicationScope.setPerApplicationData(SampleManager.class, "inner");
      });

      Assert.assertEquals(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class), "outer");
    });
  }

  public void testGetReturnsNullForUnsetKey () {

    Assert.assertNull(new PerApplicationScope().call(() -> PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));
  }

  @Test(expectedExceptions = ClassCastException.class)
  public void testGetRejectsWrongType () {

    new PerApplicationScope().run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "value");
      PerApplicationScope.getPerApplicationData(SampleManager.class, Integer.class);
    });
  }

  public void testCallPropagatesCheckedException () {

    try {
      new PerApplicationScope().call(() -> {
        throw new IOException("expected");
      });
      Assert.fail("The operation should have thrown");
    } catch (IOException ioException) {
      Assert.assertEquals(ioException.getMessage(), "expected");
    }
  }

  public void testPlainChildThreadIsNotBound () {

    AtomicBoolean childWasUnbound = new AtomicBoolean(false);

    new PerApplicationScope().run(() -> joinChild(() -> {
      try {
        PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {
        childWasUnbound.set(true);
      }
    }));

    Assert.assertTrue(childWasUnbound.get());
  }

  public void testCarrierRebindsTheSameMapOnAnotherThread () {

    AtomicReference<String> seen = new AtomicReference<>();
    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {

      PerApplicationScope.ApplicationCarrier carrier = PerApplicationScope.generateCarrier();

      PerApplicationScope.setPerApplicationData(SampleManager.class, "after-capture");

      joinChild(() -> carrier.run(() -> {
        seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
        PerApplicationScope.setPerApplicationData(OtherManager.class, "from-child");
      }));
    });

    Assert.assertEquals(seen.get(), "after-capture");
    Assert.assertEquals(scope.call(() -> PerApplicationScope.getPerApplicationData(OtherManager.class, String.class)), "from-child");
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testGenerateCarrierWithoutScopeThrows () {

    PerApplicationScope.generateCarrier();
  }

  public void testWrappedThreadFactoryBindsEveryThreadItCreates () {

    AtomicReference<String> seen = new AtomicReference<>();
    AtomicReference<String> threadName = new AtomicReference<>();

    new PerApplicationScope().run(() -> {

      ThreadFactory wrapped;

      PerApplicationScope.setPerApplicationData(SampleManager.class, "value");

      wrapped = PerApplicationScope.wrapThreadFactory(runnable -> {

        Thread thread = new Thread(runnable);

        thread.setName("per-application-scope-test-worker");
        thread.setDaemon(true);

        return thread;
      });

      joinThread(wrapped.newThread(() -> {
        seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class));
        threadName.set(Thread.currentThread().getName());
      }));
    });

    Assert.assertEquals(seen.get(), "value");
    Assert.assertEquals(threadName.get(), "per-application-scope-test-worker");
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testWrapThreadFactoryWithoutScopeThrows () {

    PerApplicationScope.wrapThreadFactory(Thread::new);
  }

  public void testWrappedExecutorCarriesScopeBoundAtSubmission () {

    LinkedList<Runnable> deferred = new LinkedList<>();
    LinkedList<String> seen = new LinkedList<>();
    Executor executor = PerApplicationScope.wrapExecutor(deferred::add);
    PerApplicationScope first = new PerApplicationScope();
    PerApplicationScope second = new PerApplicationScope();

    first.run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "first");
      executor.execute(() -> seen.add(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));
    });
    second.run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "second");
      executor.execute(() -> seen.add(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));
    });

    for (Runnable runnable : deferred) {
      joinChild(runnable);
    }

    Assert.assertEquals(seen, List.of("first", "second"));
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testWrappedExecutorThrowsAtSubmissionWithoutScope () {

    Executor executor = PerApplicationScope.wrapExecutor(Runnable::run);

    executor.execute(() -> {
    });
  }

  public void testBoundThreadFactoryBindsThisScopeWithoutAnyCurrentBinding () {

    AtomicReference<String> seen = new AtomicReference<>();
    PerApplicationScope scope = new PerApplicationScope();
    ThreadFactory threadFactory = scope.boundThreadFactory(PerApplicationScopeTest::namedThread);

    scope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "owned"));

    joinThread(threadFactory.newThread(() -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class))));

    Assert.assertEquals(seen.get(), "owned");
  }

  public void testBoundThreadFactoryIgnoresTheCallingThreadsBinding () {

    AtomicReference<String> seen = new AtomicReference<>();
    PerApplicationScope owned = new PerApplicationScope();
    PerApplicationScope other = new PerApplicationScope();
    ThreadFactory threadFactory = owned.boundThreadFactory(PerApplicationScopeTest::namedThread);

    owned.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "owned"));
    other.run(() -> {
      PerApplicationScope.setPerApplicationData(SampleManager.class, "other");
      joinThread(threadFactory.newThread(() -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class))));
    });

    Assert.assertEquals(seen.get(), "owned");
  }

  public void testBoundExecutorBindsThisScopeAroundEachTask () {

    LinkedList<Runnable> deferred = new LinkedList<>();
    AtomicReference<String> seen = new AtomicReference<>();
    PerApplicationScope scope = new PerApplicationScope();
    Executor executor = scope.boundExecutor(deferred::add);

    scope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "owned"));
    executor.execute(() -> seen.set(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class)));

    Assert.assertNull(seen.get());
    joinChild(deferred.getFirst());
    Assert.assertEquals(seen.get(), "owned");
  }

  public void testGetBoundIsNullWhenNothingIsBound () {

    Assert.assertNull(PerApplicationScope.getBound());
  }

  public void testGetBoundReturnsTheScopeInsideRunAndCall () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> Assert.assertSame(PerApplicationScope.getBound(), scope));
    Assert.assertSame(scope.call(PerApplicationScope::getBound), scope);
    Assert.assertNull(PerApplicationScope.getBound(), "the binding should end with the call");
  }

  public void testGetBoundReturnsTheInnermostScopeWhenNested () {

    PerApplicationScope outer = new PerApplicationScope();
    PerApplicationScope inner = new PerApplicationScope();

    outer.run(() -> {
      inner.run(() -> Assert.assertSame(PerApplicationScope.getBound(), inner));
      Assert.assertSame(PerApplicationScope.getBound(), outer);
    });
  }

  public void testGetBoundReturnsTheScopeOnCarrierAndFactoryThreads () {

    PerApplicationScope scope = new PerApplicationScope();
    AtomicReference<PerApplicationScope> seenThroughCarrier = new AtomicReference<>();
    AtomicReference<PerApplicationScope> seenOnFactoryThread = new AtomicReference<>();

    scope.run(() -> {

      PerApplicationScope.ApplicationCarrier carrier = PerApplicationScope.generateCarrier();

      joinChild(() -> carrier.run(() -> seenThroughCarrier.set(PerApplicationScope.getBound())));
    });
    joinThread(scope.boundThreadFactory(PerApplicationScopeTest::namedThread).newThread(() -> seenOnFactoryThread.set(PerApplicationScope.getBound())));

    Assert.assertSame(seenThroughCarrier.get(), scope);
    Assert.assertSame(seenOnFactoryThread.get(), scope);
  }

  public void testResolveReturnsTheHandedScopeWhenNothingIsBound () {

    PerApplicationScope handed = new PerApplicationScope();

    Assert.assertSame(PerApplicationScope.resolve(handed), handed);
  }

  public void testResolveReturnsTheHandedScopeWhenItIsTheBoundScope () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> Assert.assertSame(PerApplicationScope.resolve(scope), scope));
  }

  public void testResolveRejectsAHandedScopeWhenAnotherScopeIsBound () {

    PerApplicationScope handed = new PerApplicationScope();
    PerApplicationScope bound = new PerApplicationScope();

    bound.run(() -> {
      try {
        PerApplicationScope.resolve(handed);
        Assert.fail("A handed scope that is not the bound scope could never see the bound registrations");
      } catch (ScopeException scopeException) {
        // expected
      }
    });
  }

  public void testResolveAdoptsTheBoundScopeForNull () {

    PerApplicationScope bound = new PerApplicationScope();

    bound.run(() -> Assert.assertSame(PerApplicationScope.resolve(null), bound));
  }

  public void testResolveReturnsNullWhenNothingIsHandedOrBound () {

    Assert.assertNull(PerApplicationScope.resolve(null));
  }

  public void testIsBoundOnCurrentThreadIsFalseWhenNothingIsBound () {

    Assert.assertFalse(PerApplicationScope.isBound());
    Assert.assertFalse(new PerApplicationScope().isBoundOnCurrentThread());
  }

  public void testIsBoundOnCurrentThreadIsTrueInsideRunAndCall () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> Assert.assertTrue(scope.isBoundOnCurrentThread()));
    Assert.assertEquals(scope.call(scope::isBoundOnCurrentThread), Boolean.TRUE);
  }

  public void testIsBoundOnCurrentThreadIsFalseInsideAnotherScopesRun () {

    PerApplicationScope scope = new PerApplicationScope();
    PerApplicationScope other = new PerApplicationScope();

    other.run(() -> {
      Assert.assertTrue(PerApplicationScope.isBound(), "some scope is bound, just not this one");
      Assert.assertFalse(scope.isBoundOnCurrentThread());
      Assert.assertTrue(other.isBoundOnCurrentThread());
    });
  }

  public void testIsBoundOnCurrentThreadIsTrueInsideACarrierTakenFromTheScope () {

    AtomicBoolean seen = new AtomicBoolean(false);
    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {

      PerApplicationScope.ApplicationCarrier carrier = PerApplicationScope.generateCarrier();

      joinChild(() -> carrier.run(() -> seen.set(scope.isBoundOnCurrentThread())));
    });

    Assert.assertTrue(seen.get());
  }

  public void testIsBoundOnCurrentThreadIsTrueOnWrappedThreadFactoryThreads () {

    AtomicBoolean seen = new AtomicBoolean(false);
    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> joinThread(PerApplicationScope.wrapThreadFactory(PerApplicationScopeTest::namedThread).newThread(() -> seen.set(scope.isBoundOnCurrentThread()))));

    Assert.assertTrue(seen.get());
  }

  public void testIsBoundOnCurrentThreadIsTrueOnBoundThreadFactoryThreads () {

    AtomicBoolean seen = new AtomicBoolean(false);
    PerApplicationScope scope = new PerApplicationScope();

    joinThread(scope.boundThreadFactory(PerApplicationScopeTest::namedThread).newThread(() -> seen.set(scope.isBoundOnCurrentThread())));

    Assert.assertTrue(seen.get());
  }

  public void testIsBoundOnCurrentThreadIsFalseAgainAfterRunReturns () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> Assert.assertTrue(scope.isBoundOnCurrentThread()));

    Assert.assertFalse(scope.isBoundOnCurrentThread());
    Assert.assertFalse(PerApplicationScope.isBound());
  }

  public void testSetPerApplicationDataIfAbsentKeepsTheFirstValue () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> {
      Assert.assertEquals(PerApplicationScope.setPerApplicationDataIfAbsent(SampleManager.class, "first"), "first");
      Assert.assertEquals(PerApplicationScope.setPerApplicationDataIfAbsent(SampleManager.class, "second"), "first");
      Assert.assertEquals(PerApplicationScope.getPerApplicationData(SampleManager.class, String.class), "first");
    });
  }

  @Test(expectedExceptions = MissingPerApplicationScopeException.class)
  public void testSetPerApplicationDataIfAbsentRequiresABoundScope () {

    PerApplicationScope.setPerApplicationDataIfAbsent(SampleManager.class, "unbound");
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class OtherManager implements PerApplicationDataManager {

  }
}

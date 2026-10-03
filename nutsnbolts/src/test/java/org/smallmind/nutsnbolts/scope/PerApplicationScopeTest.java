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

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class OtherManager implements PerApplicationDataManager {

  }
}

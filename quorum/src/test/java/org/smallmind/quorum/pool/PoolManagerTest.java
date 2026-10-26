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
package org.smallmind.quorum.pool;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the bridge semantics of {@link PoolManager}.
 */
@Test(groups = "unit")
public class PoolManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(PoolContextManager.class, new StubPool());
  }

  public void testRegisterWithBothBoundWritesBothRegistries () {

    PerApplicationScope scope = new PerApplicationScope();
    Pool pool = new StubPool();

    scope.run(() -> PoolManager.register(pool));

    Assert.assertSame(scope.call(PoolScopeManager::getPool), pool);
    Assert.assertSame(PoolContextManager.getPool(), pool);
  }

  public void testLookupFallsBackToTheContextWhenTheScopeHoldsNothing () {

    PerApplicationScope scope = new PerApplicationScope();
    Pool pool = new StubPool();

    PoolContextManager.register(pool);

    Assert.assertSame(scope.call(PoolManager::getPool), pool);
  }

  public void testLookupPrefersTheScopeWhenBothHoldAValue () {

    PerApplicationScope scope = new PerApplicationScope();
    Pool scoped = new StubPool();
    Pool context = new StubPool();

    scope.run(() -> PoolScopeManager.register(scoped));
    PoolContextManager.register(context);

    Assert.assertSame(scope.call(PoolManager::getPool), scoped);
    Assert.assertSame(PoolManager.getPool(), context);
  }

  public void testScopeOnlyThreadResolvesWithoutAContext ()
    throws Exception {

    PerApplicationScope scope = new PerApplicationScope();
    Pool pool = new StubPool();

    Assert.assertSame(onIsolatedThread(() -> scope.call(() -> {
      PoolManager.register(pool);

      return PoolManager.getPool();
    })), pool);
    Assert.assertNull(onIsolatedThread(() -> new PerApplicationScope().call(PoolManager::getPool)), "nothing registered on a scope-only thread is null, not an exception");
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testUnboundThreadFailsThroughTheContextFlavor ()
    throws Exception {

    onIsolatedThread(PoolManager::getPool);
  }

  // The test thread carries an inherited PerApplicationContext shared with every other test on it, so the scope-only
  // and unbound cases can only be observed on a thread that does not inherit it.
  private static <T> T onIsolatedThread (Callable<T> body)
    throws Exception {

    AtomicReference<T> result = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        result.set(body.call());
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    if (failure.get() != null) {
      if (failure.get() instanceof Exception) {
        throw (Exception)failure.get();
      }
      throw new RuntimeException(failure.get());
    }

    return result.get();
  }

  private static class StubPool extends Pool {

  }
}

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
package org.smallmind.claxon.registry;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the bridge semantics of {@link ClaxonRegistryManager}. Every test runs on a thread that inherits no
 * {@link PerApplicationContext}, so each starts from an empty context (or none) and leaks nothing into the map shared
 * by the other tests on the calling thread.
 */
@Test(groups = "unit")
public class ClaxonRegistryManagerTest {

  private static <T> T inFreshContext (Callable<T> body)
    throws Exception {

    return onIsolatedThread(() -> {
      new PerApplicationContext();

      return body.call();
    });
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
      throw (Error)failure.get();
    }

    return result.get();
  }

  public void testRegisterWithBothBoundWritesBothRegistries ()
    throws Exception {

    inFreshContext(() -> {

      PerApplicationScope scope = new PerApplicationScope();
      ClaxonRegistry registry = new ClaxonRegistry(new ClaxonConfiguration());

      try {
        scope.run(() -> ClaxonRegistryManager.register(registry));

        Assert.assertSame(scope.call(ClaxonRegistryScopeManager::getRegistry), registry);
        Assert.assertSame(ClaxonRegistryContextManager.getRegistry(), registry);
        Assert.assertSame(ClaxonRegistryManager.getRegistry(), registry, "visible from a thread carrying only the context");
      } finally {
        registry.stop();
      }

      return null;
    });
  }

  public void testLookupFallsBackToTheContextWhenTheScopeHoldsNothing ()
    throws Exception {

    inFreshContext(() -> {

      PerApplicationScope scope = new PerApplicationScope();
      ClaxonRegistry registry = new ClaxonRegistry(new ClaxonConfiguration());

      try {
        ClaxonRegistryContextManager.register(registry);

        Assert.assertSame(scope.call(ClaxonRegistryManager::getRegistry), registry);
      } finally {
        registry.stop();
      }

      return null;
    });
  }

  public void testLookupPrefersTheScopeWhenBothHoldAValue ()
    throws Exception {

    inFreshContext(() -> {

      PerApplicationScope scope = new PerApplicationScope();
      ClaxonRegistry scoped = new ClaxonRegistry(new ClaxonConfiguration());
      ClaxonRegistry context = new ClaxonRegistry(new ClaxonConfiguration());

      try {
        scope.run(() -> ClaxonRegistryScopeManager.register(scoped));
        ClaxonRegistryContextManager.register(context);

        Assert.assertSame(scope.call(ClaxonRegistryManager::getRegistry), scoped);
        Assert.assertSame(ClaxonRegistryManager.getRegistry(), context);
      } finally {
        scoped.stop();
        context.stop();
      }

      return null;
    });
  }

  public void testScopeOnlyThreadResolvesWithoutAContext ()
    throws Exception {

    PerApplicationScope scope = new PerApplicationScope();

    Assert.assertNull(onIsolatedThread(() -> scope.call(ClaxonRegistryManager::getRegistry)), "nothing registered on a scope-only thread is null, not an exception");
  }

  public void testUnboundThreadResolvesToNullSoMetricsNoOp ()
    throws Exception {

    Assert.assertNull(onIsolatedThread(ClaxonRegistryManager::getRegistry));
    Assert.assertTrue(onIsolatedThread(() -> Instrument.with(ClaxonRegistryManagerTest.class, null) instanceof UnpluggedInstrumentation));
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testRegisterOnAnUnboundThreadStillFails ()
    throws Exception {

    ClaxonRegistry registry = new ClaxonRegistry(new ClaxonConfiguration());

    try {
      onIsolatedThread(() -> {
        ClaxonRegistryManager.register(registry);

        return null;
      });
    } finally {
      registry.stop();
    }
  }
}

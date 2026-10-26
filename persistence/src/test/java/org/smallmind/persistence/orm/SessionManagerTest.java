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
package org.smallmind.persistence.orm;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.Mockito;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.MissingPerApplicationContextException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the bridge semantics of {@link SessionManager}: registration lands in every bound registry, lookups read
 * the scope first and fall back to the context, and the throwing getters wrap the non-throwing probe.
 */
@Test(groups = "unit")
public class SessionManagerTest {

  @BeforeMethod
  public void attachApplicationContext () {

    new PerApplicationContext();
    PerApplicationContext.setPerApplicationData(SessionContextManager.class, new ConcurrentHashMap<>());
  }

  public void testRegisterWithBothBoundWritesBothRegistries () {

    PerApplicationScope scope = new PerApplicationScope();
    ProxySession proxySession = Mockito.mock(ProxySession.class);

    scope.run(() -> SessionManager.register("primary", proxySession));

    Assert.assertSame(scope.call(() -> SessionScopeManager.getSession("primary")), proxySession);
    Assert.assertSame(SessionContextManager.getSession("primary"), proxySession);
    Assert.assertSame(SessionManager.getSession("primary"), proxySession);
  }

  public void testLookupFallsBackToTheContextWhenTheScopeHoldsNothing () {

    PerApplicationScope scope = new PerApplicationScope();
    ProxySession proxySession = Mockito.mock(ProxySession.class);

    SessionContextManager.register(null, proxySession);

    Assert.assertSame(scope.call(SessionManager::getDefaultSession), proxySession);
    Assert.assertSame(scope.call(() -> SessionManager.findSession(null)), proxySession);
  }

  public void testLookupPrefersTheScopeWhenBothHoldAValue () {

    PerApplicationScope scope = new PerApplicationScope();
    ProxySession scopedSession = Mockito.mock(ProxySession.class);
    ProxySession contextSession = Mockito.mock(ProxySession.class);

    scope.run(() -> SessionScopeManager.register("primary", scopedSession));
    SessionContextManager.register("primary", contextSession);

    Assert.assertSame(scope.call(() -> SessionManager.getSession("primary")), scopedSession);
    Assert.assertSame(SessionManager.getSession("primary"), contextSession);
  }

  @Test(expectedExceptions = ORMInitializationException.class)
  public void testMissingInEveryBoundRegistryThrows () {

    PerApplicationScope scope = new PerApplicationScope();

    scope.run(() -> SessionManager.getSession("missing"));
  }

  public void testFindReturnsNullInsteadOfThrowing () {

    PerApplicationScope scope = new PerApplicationScope();

    Assert.assertNull(scope.call(() -> SessionManager.findSession("missing")));
    Assert.assertNull(SessionManager.findSession("missing"));
  }

  public void testScopeOnlyThreadResolvesWithoutAContext ()
    throws Exception {

    PerApplicationScope scope = new PerApplicationScope();
    ProxySession proxySession = Mockito.mock(ProxySession.class);

    Assert.assertSame(onIsolatedThread(() -> scope.call(() -> {
      SessionManager.register("primary", proxySession);

      return SessionManager.getSession("primary");
    })), proxySession);
    Assert.assertNull(onIsolatedThread(() -> scope.call(() -> SessionManager.findSession("missing"))), "an unregistered key on a scope-only thread is null, not an exception");
  }

  @Test(expectedExceptions = MissingPerApplicationContextException.class)
  public void testUnboundThreadFailsThroughTheContextFlavor ()
    throws Exception {

    onIsolatedThread(() -> SessionManager.findSession("primary"));
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
}

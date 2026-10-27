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
package org.smallmind.phalanx.wire.transport.mock;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Proves how a {@link MockMessageRouter} settles the scope its dispatch threads run inside, once, on the thread that
 * constructs it: built with {@code null} inside a scope it adopts that scope, built with the very scope that is bound
 * it keeps it, built with a different scope than the one bound it fails, and built with {@code null} on a thread
 * bound to nothing its threads run unbound. The listener reports what the dispatch thread itself sees, so the
 * assertion is on the thread's binding rather than the constructing thread's.
 */
@Test(groups = "unit")
public class MockMessageRouterScopeAdoptionTest {

  private static final long DISPATCH_WAIT_SECONDS = 5;

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

  private static PerApplicationScope scopeBoundOnTheDispatchThreadOf (MockMessageRouter messageRouter)
    throws InterruptedException {

    try {

      AtomicReference<PerApplicationScope> seen = new AtomicReference<>();
      CountDownLatch latch = new CountDownLatch(1);

      messageRouter.getTalkRequestQueue().addListener(new MockMessageListener() {

        @Override
        public boolean match (MockMessageProperties properties) {

          return true;
        }

        @Override
        public void handle (MockMessage message) {

          seen.set(PerApplicationScope.getBound());
          latch.countDown();
        }
      });
      messageRouter.getTalkRequestQueue().send(new MockMessage(new byte[0]));

      Assert.assertTrue(latch.await(DISPATCH_WAIT_SECONDS, TimeUnit.SECONDS), "the dispatch thread did not deliver the message in time");

      return seen.get();
    } finally {
      messageRouter.close();
    }
  }

  @Test
  public void testRouterGivenNoScopeAdoptsTheScopeBoundOnTheConstructingThread ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    MockMessageRouter messageRouter = onIsolatedThread(() -> perApplicationScope.call(() -> new MockMessageRouter(null)));

    Assert.assertSame(scopeBoundOnTheDispatchThreadOf(messageRouter), perApplicationScope, "the dispatch thread must run inside the scope the router was constructed in");
  }

  @Test
  public void testRouterGivenTheBoundScopeRunsInsideIt ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    MockMessageRouter messageRouter = onIsolatedThread(() -> perApplicationScope.call(() -> new MockMessageRouter(perApplicationScope)));

    Assert.assertSame(scopeBoundOnTheDispatchThreadOf(messageRouter), perApplicationScope);
  }

  @Test
  public void testRouterGivenAScopeOnAnUnboundThreadRunsInsideThatScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    MockMessageRouter messageRouter = onIsolatedThread(() -> {
      Assert.assertFalse(PerApplicationScope.isBound(), "the constructing thread must carry no scope");

      return new MockMessageRouter(perApplicationScope);
    });

    Assert.assertSame(scopeBoundOnTheDispatchThreadOf(messageRouter), perApplicationScope);
  }

  @Test
  public void testRouterGivenNoScopeOnAnUnboundThreadRunsUnbound ()
    throws Exception {

    MockMessageRouter messageRouter = onIsolatedThread(() -> {
      Assert.assertFalse(PerApplicationScope.isBound(), "the constructing thread must carry no scope");

      return new MockMessageRouter(null);
    });

    Assert.assertNull(scopeBoundOnTheDispatchThreadOf(messageRouter), "a router built with no scope on an unbound thread must dispatch unbound");
  }

  @Test(expectedExceptions = ScopeException.class)
  public void testRouterGivenADifferentScopeThanTheOneBoundIsRejected ()
    throws Exception {

    onIsolatedThread(() -> new PerApplicationScope().call(() -> new MockMessageRouter(new PerApplicationScope())));
  }
}

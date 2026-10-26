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
import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.context.ContextFactory;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeFactory;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.TestWireScope;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.phalanx.wire.WireScopeManager;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Carries a {@link TestWireContext} and a {@link TestWireScope} on the same call over the mock transport. The receiver
 * resolves the context handle through the {@link PerApplicationContext} its dispatch threads inherited and the scope
 * handle through the {@link PerApplicationScope} its router was given, so the service sees both bound.
 */
@Test(groups = "unit")
public class MockWireBothBindingsTest {

  @Test
  public void testContextAndScopeCarriedTogetherAreBothBoundOnTheServiceThread ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    MockMessageRouter messageRouter;
    JsonSignalCodec signalCodec = new JsonSignalCodec();
    MockResponseTransport responseTransport;
    MockRequestTransport requestTransport;

    perApplicationScope.run(() -> WireScopeManager.register("test", TestWireScope.class));

    // The dispatch threads are created by the router, so a router built on a thread carrying the context
    // registration leaves them with an inherited copy of it.
    messageRouter = onIsolatedThread(() -> {
      new PerApplicationContext();
      WireContextManager.register("test", TestWireContext.class);

      return new MockMessageRouter(perApplicationScope);
    });

    responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    requestTransport = new MockRequestTransport(messageRouter, signalCodec, 5);

    try {

      WireTestingService wireTestingService;

      responseTransport.register(WireTestingService.class, new WireTestingServiceImpl());
      wireTestingService = (WireTestingService)WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("test"), null, null);

      ContextFactory.pushContext(new TestWireContext("flibble"));
      try {
        Assert.assertTrue(ScopeFactory.<Boolean, RuntimeException>callInScope(new TestWireScope("flibble"), wireTestingService::hasContext), "the context must be bound on the service thread when carried alongside a scope");
        Assert.assertTrue(ScopeFactory.<Boolean, RuntimeException>callInScope(new TestWireScope("flibble"), wireTestingService::hasScope), "the scope must be bound on the service thread when carried alongside a context");
      } finally {
        ContextFactory.popContext(TestWireContext.class);
      }
    } finally {
      requestTransport.close();
      responseTransport.close();
      messageRouter.close();
    }
  }

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

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

import java.util.concurrent.atomic.AtomicReference;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.Scope;
import org.smallmind.nutsnbolts.scope.ScopeFactory;
import org.smallmind.phalanx.wire.OtherTestWireScope;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireScope;
import org.smallmind.phalanx.wire.WireScopeManager;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.signal.ProtoWireScope;
import org.smallmind.phalanx.wire.signal.WireScope;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Scopes are keyed by class on both ends of the wire, so of several instances of one {@link WireScope} class bound
 * around a call only the last survives, and handles the receiver does not know, which all arrive as
 * {@link ProtoWireScope}, collapse to one on the service thread. The service records every wire scope bound when its
 * method runs, so the assertion is on what the receiving thread actually sees.
 */
@Test(groups = "unit")
public class MockWireScopeCollapseTest {

  @Test
  public void testTwoInstancesOfOneScopeClassCollapseToTheLastBound ()
    throws Exception {

    PerApplicationScope routerScope = new PerApplicationScope();
    Scope[] seen;

    routerScope.run(() -> WireScopeManager.register("test", TestWireScope.class));

    seen = scopesSeenBy(routerScope, new Scope[] {new TestWireScope("first"), new TestWireScope("last")});

    Assert.assertEquals(seen.length, 1, "exactly one wire scope must reach the service thread");
    Assert.assertTrue(seen[0] instanceof TestWireScope, "expected the registered class, saw " + seen[0]);
    Assert.assertEquals(((TestWireScope)seen[0]).getMessage(), "last", "the last instance bound must be the one that survives");
  }

  // The router's scope registers no handles, so both scopes arrive as ProtoWireScope and share one class key.
  @Test
  public void testTwoUnregisteredHandlesArriveAsProtoWireScopesAndCollapseToOne ()
    throws Exception {

    Scope[] seen = scopesSeenBy(new PerApplicationScope(), new Scope[] {new TestWireScope("flibble"), new OtherTestWireScope("flobble")});
    String skin;

    Assert.assertEquals(seen.length, 1, "exactly one wire scope must reach the service thread");
    Assert.assertTrue(seen[0] instanceof ProtoWireScope, "an unregistered handle must arrive as a ProtoWireScope, saw " + seen[0]);

    skin = ((ProtoWireScope)seen[0]).getSkin();
    Assert.assertTrue("test".equals(skin) || "other".equals(skin), "the surviving handle must be one of the two sent, saw " + skin);
  }

  private static Scope[] scopesSeenBy (PerApplicationScope routerScope, Scope[] callerScopes)
    throws Exception {

    MockMessageRouter messageRouter = new MockMessageRouter(routerScope);
    JsonSignalCodec signalCodec = new JsonSignalCodec();
    MockResponseTransport responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    MockRequestTransport requestTransport = new MockRequestTransport(messageRouter, signalCodec, 5);

    try {

      ScopeRecordingService service = new ScopeRecordingService();
      WireTestingService wireTestingService;

      responseTransport.register(WireTestingService.class, service);
      wireTestingService = (WireTestingService)WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("test"), null, null);

      ScopeFactory.<Boolean, RuntimeException>callInScopes(callerScopes, wireTestingService::hasScope);

      return service.seenScopes.get();
    } finally {
      requestTransport.close();
      responseTransport.close();
      messageRouter.close();
    }
  }

  // Records every wire scope bound on the service thread at the moment hasScope() runs.
  private static class ScopeRecordingService extends WireTestingServiceImpl {

    private final AtomicReference<Scope[]> seenScopes = new AtomicReference<>();

    @Override
    public boolean hasScope () {

      try {
        seenScopes.set(ScopeFactory.filterScopesOn(WireTestingService.class.getMethod("hasScope"), WireScope.class));
      } catch (NoSuchMethodException noSuchMethodException) {
        throw new RuntimeException(noSuchMethodException);
      }

      return super.hasScope();
    }
  }
}

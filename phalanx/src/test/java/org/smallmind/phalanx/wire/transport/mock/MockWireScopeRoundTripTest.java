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

import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeFactory;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireScope;
import org.smallmind.phalanx.wire.WireScopeManager;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.WireTestingServiceImpl;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Carries a {@link TestWireScope} across the mock transport. The handle is registered through
 * {@link WireScopeManager} inside a {@link PerApplicationScope}, the router dispatches inside that same scope so the
 * receiver can resolve the handle while decoding, and the service reports whether the scope was bound on its own
 * thread when the method ran.
 */
@Test(groups = "unit")
public class MockWireScopeRoundTripTest {

  private static PerApplicationScope scopeKnowingTheTestHandle () {

    PerApplicationScope perApplicationScope = new PerApplicationScope();

    perApplicationScope.run(() -> WireScopeManager.register("test", TestWireScope.class));

    return perApplicationScope;
  }

  private static boolean serviceSawScope (boolean bindCallerScope)
    throws Exception {

    MockMessageRouter messageRouter = new MockMessageRouter(scopeKnowingTheTestHandle());
    JsonSignalCodec signalCodec = new JsonSignalCodec();
    MockResponseTransport responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    MockRequestTransport requestTransport = new MockRequestTransport(messageRouter, signalCodec, 5);

    try {

      WireTestingService wireTestingService;

      responseTransport.register(WireTestingService.class, new WireTestingServiceImpl());
      wireTestingService = (WireTestingService)WireProxyFactory.generateProxy(requestTransport, 1, "WireTestService", WireTestingService.class, new StaticParameterExtractor<>("test"), null, null);

      if (bindCallerScope) {

        return ScopeFactory.callInScope(new TestWireScope("flibble"), wireTestingService::hasScope);
      }

      return wireTestingService.hasScope();
    } finally {
      requestTransport.close();
      responseTransport.close();
      messageRouter.close();
    }
  }

  @Test
  public void testScopeBoundOnTheCallerIsBoundOnTheServiceThread ()
    throws Exception {

    Assert.assertTrue(serviceSawScope(true));
  }

  @Test
  public void testNoScopeOnTheCallerMeansNoScopeOnTheServiceThread ()
    throws Exception {

    Assert.assertFalse(serviceSawScope(false));
  }
}

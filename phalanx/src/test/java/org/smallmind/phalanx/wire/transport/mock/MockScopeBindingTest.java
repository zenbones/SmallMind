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

import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.phalanx.wire.Argument;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.signal.JsonSignalCodec;
import org.smallmind.phalanx.wire.spring.WireProxyFactory;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Proves that a {@link MockMessageRouter} built with a {@link PerApplicationScope} runs the service invocation on a
 * dispatch thread bound to that scope, and that one built without leaves the thread unbound. The service reports
 * what its own thread sees, so the assertion is on the worker thread's binding rather than the caller's.
 */
@Test(groups = "unit")
public class MockScopeBindingTest {

  private static String invokeOver (MockMessageRouter messageRouter)
    throws Exception {

    JsonSignalCodec signalCodec = new JsonSignalCodec();
    MockResponseTransport responseTransport = new MockResponseTransport(messageRouter, signalCodec);
    MockRequestTransport requestTransport = new MockRequestTransport(messageRouter, signalCodec, 5);

    try {

      String instanceId = responseTransport.register(ScopeProbeService.class, new ScopeProbeServiceImpl());
      ScopeProbeService serviceProxy = (ScopeProbeService)WireProxyFactory.generateProxy(requestTransport, 1, "ScopeProbeService", ScopeProbeService.class, new StaticParameterExtractor<>("group"), new StaticParameterExtractor<>(instanceId), null);

      return serviceProxy.probe("x");
    } finally {
      requestTransport.close();
      responseTransport.close();
      messageRouter.close();
    }
  }

  public interface ScopeProbeService {

    String probe (@Argument("value") String value);
  }

  @Test
  public void testServiceRunsInsideTheScopeWhenTheRouterIsGivenOne ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Assert.assertEquals(invokeOver(new MockMessageRouter(perApplicationScope)), "scoped");
  }

  @Test
  public void testServiceIsUnboundWhenTheRouterHasNoScope ()
    throws Exception {

    Assert.assertEquals(invokeOver(new MockMessageRouter(null)), "unbound");
  }

  public static class SampleManager implements PerApplicationDataManager {

  }

  public static class ScopeProbeServiceImpl implements ScopeProbeService, WiredService {

    @Override
    public int getVersion () {

      return 1;
    }

    @Override
    public String getServiceName () {

      return "ScopeProbeService";
    }

    @Override
    public void setResponseTransport (ResponseTransport responseTransport) {

    }

    @Override
    public String probe (String value) {

      try {

        return PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
      } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {

        return "unbound";
      }
    }
  }
}

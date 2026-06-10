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
package org.smallmind.phalanx.wire.spring;

import java.util.Map;
import org.smallmind.phalanx.wire.StaticParameterExtractor;
import org.smallmind.phalanx.wire.TestWireContext;
import org.smallmind.phalanx.wire.WireContextManager;
import org.smallmind.phalanx.wire.WireTestingService;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.transport.RequestTransport;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Exercises the Spring assembly helpers as direct objects (no container): each
 * {@code FactoryBean}'s {@code afterPropertiesSet}/{@code getObject} pair and the
 * {@link WireContextInitializingBean} registration side effect.
 */
@Test(groups = "unit")
public class WireSpringFactoryBeanTest {

  @Test
  public void testStaticParameterExtractorFactoryBean ()
    throws Exception {

    StaticParameterExtractorFactoryBean factoryBean = new StaticParameterExtractorFactoryBean();

    factoryBean.setParameter("fixed-group");
    factoryBean.afterPropertiesSet();

    Assert.assertEquals(factoryBean.getObjectType(), StaticParameterExtractor.class);
    Assert.assertTrue(factoryBean.isSingleton());
    Assert.assertEquals(factoryBean.getObject().getParameter(null, null), "fixed-group");
  }

  @Test
  public void testWireContextInitializingBeanRegistersHandle ()
    throws Exception {

    WireContextInitializingBean initializingBean = new WireContextInitializingBean();

    initializingBean.setHandle("spring-bean-handle");
    initializingBean.setContextClass(TestWireContext.class);
    initializingBean.afterPropertiesSet();

    Assert.assertEquals(WireContextManager.getContextClass("spring-bean-handle"), TestWireContext.class);
  }

  @Test
  public void testWireProxyFactoryBeanBuildsAWorkingProxy ()
    throws Exception {

    CapturingRequestTransport transport = new CapturingRequestTransport();
    WireProxyFactoryBean factoryBean = new WireProxyFactoryBean();

    factoryBean.setRequestTransport(transport);
    factoryBean.setVersion(1);
    factoryBean.setServiceName("WireTestService");
    factoryBean.setServiceInterface(WireTestingService.class);
    factoryBean.setServiceGroupExtractor(new StaticParameterExtractor<>("group"));
    factoryBean.afterPropertiesSet();

    Assert.assertEquals(factoryBean.getObjectType(), WireTestingService.class);
    Assert.assertTrue(factoryBean.isSingleton());
    Assert.assertTrue(factoryBean.getObject() instanceof WireTestingService);

    ((WireTestingService)factoryBean.getObject()).echoString("ping");

    Assert.assertNotNull(transport.route);
    Assert.assertEquals(transport.route.getFunction().getName(), "echoString");
  }

  private static class CapturingRequestTransport implements RequestTransport {

    private Route route;

    @Override
    public String getCallerId () {

      return "caller";
    }

    @Override
    public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts) {

      this.route = route;

      return "echoed";
    }

    @Override
    public void completeCallback (String correlationId, ResultSignal resultSignal) {

    }

    @Override
    public void close () {

    }
  }
}

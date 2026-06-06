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
package org.smallmind.nutsnbolts.spring.remote;

import java.net.ServerSocket;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMISocketFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "integration")
public class RMIRegistryFactoryBeanTest {

  private static int findFreePort ()
    throws Exception {

    try (ServerSocket socket = new ServerSocket(0)) {

      return socket.getLocalPort();
    }
  }

  public void testFactoryReportsSingletonRegistryType () {

    RMIRegistryFactoryBean factory = new RMIRegistryFactoryBean();

    Assert.assertEquals(factory.getObjectType(), Registry.class);
    Assert.assertTrue(factory.isSingleton());
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testMismatchedSocketFactoriesRejected ()
    throws Exception {

    int port = findFreePort();
    RMIRegistryFactoryBean factory = new RMIRegistryFactoryBean();

    factory.setHost(null);
    factory.setPort(port);
    factory.setClientSocketFactory((RMIClientSocketFactory)RMISocketFactory.getDefaultSocketFactory());

    factory.afterPropertiesSet();
  }

  public void testFactoryCreatesAndExposesLocalRegistry ()
    throws Exception {

    int port = findFreePort();
    RMIRegistryFactoryBean factory = new RMIRegistryFactoryBean();

    factory.setHost(null);
    factory.setPort(port);

    factory.afterPropertiesSet();

    Registry registry = factory.getObject();
    Assert.assertNotNull(registry);
    Assert.assertNotNull(registry.list());
  }
}

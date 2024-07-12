/*
 * Copyright (c) 2007 through 2024 David Berkman
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
package org.smallmind.sso.oauth.v2dot0;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.HttpUrlConnectorProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test
public class OAuthResourceTest extends JerseyTest {

  private final CountDownLatch terminalLatch = new CountDownLatch(1);
  private ClassPathXmlApplicationContext idpContext;
  private ClassPathXmlApplicationContext ownerContext;
  private ClassPathXmlApplicationContext reliantContext;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    setUp();

    idpContext = new ClassPathXmlApplicationContext("org/smallmind/sso/oauth/v2dot0/idp.xml");
    ownerContext = new ClassPathXmlApplicationContext("org/smallmind/sso/oauth/v2dot0/owner.xml");
    reliantContext = new ClassPathXmlApplicationContext("org/smallmind/sso/oauth/v2dot0/reliant.xml");
  }

  @AfterClass(alwaysRun = true)
  public void afterClass ()
    throws Exception {

    reliantContext.close();
    ownerContext.close();
    idpContext.close();

    tearDown();
  }

  @Override
  protected TestContainerFactory getTestContainerFactory ()
    throws TestContainerException {

    return new ExternalTestContainerFactory();
  }

  @Override
  protected ResourceConfig configure () {

    return new ResourceConfig();
  }

  @Override
  protected void configureClient (ClientConfig clientConfig) {

    clientConfig.property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
      .register(new LoggingFeature(Logger.getGlobal(), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 8192));

    super.configureClient(clientConfig);
  }

  @Test
  public void test ()
    throws InterruptedException {

    terminalLatch.await();
  }
}

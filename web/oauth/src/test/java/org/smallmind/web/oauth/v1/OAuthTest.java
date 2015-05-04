/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015 David Berkman
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
 * 2) The terms of the MIT license as published by the Open Source
 * Initiative
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License or MIT License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * and the MIT License along with the SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/> or <http://opensource.org/licenses/MIT>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.web.oauth.v1;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.smallmind.nutsnbolts.lang.PerApplicationContext;
import org.smallmind.web.jersey.util.JsonCodec;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "integ")
public class OAuthTest extends JerseyTest {

  private ClassPathXmlApplicationContext context;

  @BeforeClass
  public void beforeClass ()
      throws Exception {

    setUp();
  }

  @AfterClass
  public void afterClass () {

    context.close();
  }

  @Override
  protected TestContainerFactory getTestContainerFactory ()
      throws TestContainerException {

    return new ExternalTestContainerFactory();
  }

  @Override
  protected ResourceConfig configure () {

    new PerApplicationContext();

    System.setProperty(TestProperties.LOG_TRAFFIC, "true");
    System.setProperty(TestProperties.DUMP_ENTITY, "true");
    System.setProperty(ExternalTestContainerFactory.JERSEY_TEST_HOST, "localhost");
    System.setProperty(TestProperties.CONTAINER_PORT, "9015");

    System.setProperty("SMALLMIND_ENVIRONMENT", "test");

    context = new ClassPathXmlApplicationContext("org/smallmind/foundation/foundation.xml", "org/smallmind/scribe/spring/test-logging.xml", "org/smallmind/web/oauth/v1/test-oauth.xml");

    return new ResourceConfig(ClientResource.class);
  }

  @Test
  public void test ()
      throws IOException {

    String rawResponse = target().path("/rest/spoof/login").request(MediaType.APPLICATION_FORM_URLENCODED).get(String.class);
    OAuthGrant oauthGrant = JsonCodec.read(rawResponse, OAuthGrant.class);

    Assert.assertNotNull(oauthGrant.getAccessToken());
  }
}

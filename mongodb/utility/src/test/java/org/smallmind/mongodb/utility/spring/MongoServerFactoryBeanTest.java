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
package org.smallmind.mongodb.utility.spring;

import com.mongodb.ServerAddress;
import org.smallmind.nutsnbolts.util.SpreadParserException;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class MongoServerFactoryBeanTest {

  public void testIsSingletonReturnsTrue () {

    Assert.assertTrue(new MongoServerFactoryBean().isSingleton());
  }

  public void testGetObjectTypeReturnsServerAddressArrayClass () {

    Assert.assertEquals(new MongoServerFactoryBean().getObjectType(), ServerAddress[].class);
  }

  public void testNullPatternLeavesGetObjectNull ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.afterPropertiesSet();

    Assert.assertNull(bean.getObject());
  }

  public void testEmptyPatternLeavesGetObjectNull ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("");
    bean.afterPropertiesSet();

    Assert.assertNull(bean.getObject());
  }

  public void testPlainHostnameProducesSingleAddressOnDefaultPort ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("localhost");
    bean.afterPropertiesSet();

    ServerAddress[] addresses = bean.getObject();

    Assert.assertEquals(addresses.length, 1);
    Assert.assertEquals(addresses[0].getHost(), "localhost");
    Assert.assertEquals(addresses[0].getPort(), 27017);
  }

  public void testPlainHostnameWithPortProducesSingleAddressOnThatPort ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("host.example.com:12345");
    bean.afterPropertiesSet();

    ServerAddress[] addresses = bean.getObject();

    Assert.assertEquals(addresses.length, 1);
    Assert.assertEquals(addresses[0].getHost(), "host.example.com");
    Assert.assertEquals(addresses[0].getPort(), 12345);
  }

  public void testPatternWithPlaceholderAndSpreadExpandsToMultipleAddresses ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("mongo#.internal");
    bean.setServerSpread("1..3");
    bean.afterPropertiesSet();

    ServerAddress[] addresses = bean.getObject();

    Assert.assertEquals(addresses.length, 3);
    Assert.assertEquals(addresses[0].getHost(), "mongo1.internal");
    Assert.assertEquals(addresses[0].getPort(), 27017);
    Assert.assertEquals(addresses[1].getHost(), "mongo2.internal");
    Assert.assertEquals(addresses[2].getHost(), "mongo3.internal");
  }

  public void testPatternWithPlaceholderAndExplicitPortAppliesPortToEveryExpansion ()
    throws SpreadParserException {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("mongo#.internal:9999");
    bean.setServerSpread("1..3");
    bean.afterPropertiesSet();

    ServerAddress[] addresses = bean.getObject();

    Assert.assertEquals(addresses.length, 3);
    for (ServerAddress address : addresses) {
      Assert.assertEquals(address.getPort(), 9999);
    }
  }

  public void testPlaceholderAfterPortColonFailsToConfigureAddresses () {

    MongoServerFactoryBean bean = new MongoServerFactoryBean();

    bean.setServerPattern("mongo.internal:#");
    bean.setServerSpread("1..3");

    Assert.assertThrows(RuntimeException.class, bean::afterPropertiesSet);
  }
}

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
package org.smallmind.kafka.utility;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link KafkaServerFactoryBean}. Exercises the four pattern-parsing
 * branches in {@link KafkaServerFactoryBean#afterPropertiesSet()} (with and without
 * a {@code #} placeholder, with and without an explicit {@code :port} suffix), the
 * documented null/empty-pattern shortcut, the documented {@code 9092} default port
 * for parsed patterns, and the two {@code FactoryBean} contract methods whose
 * answers are part of how Spring assembles the array.
 */
@Test(groups = "unit")
public class KafkaServerFactoryBeanTest {

  public void testGetObjectBeforeInitializationReturnsNull () {

    Assert.assertNull(new KafkaServerFactoryBean().getObject());
  }

  public void testIsSingletonReturnsTrue () {

    Assert.assertTrue(new KafkaServerFactoryBean().isSingleton());
  }

  public void testGetObjectTypeIsKafkaServerArrayClass () {

    Assert.assertEquals(new KafkaServerFactoryBean().getObjectType(), KafkaServer[].class);
  }

  public void testNullPatternLeavesArrayUnset ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.afterPropertiesSet();

    Assert.assertNull(factory.getObject());
  }

  public void testEmptyPatternLeavesArrayUnset ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("");
    factory.afterPropertiesSet();

    Assert.assertNull(factory.getObject());
  }

  public void testPlainHostnameDefaultsToPort9092 ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("broker.internal");
    factory.afterPropertiesSet();

    KafkaServer[] servers = factory.getObject();

    Assert.assertNotNull(servers);
    Assert.assertEquals(servers.length, 1);
    Assert.assertEquals(servers[0].getHost(), "broker.internal");
    Assert.assertEquals(servers[0].getPort(), 9092);
  }

  public void testHostnameWithExplicitPortIsParsed ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("broker.internal:9094");
    factory.afterPropertiesSet();

    KafkaServer[] servers = factory.getObject();

    Assert.assertNotNull(servers);
    Assert.assertEquals(servers.length, 1);
    Assert.assertEquals(servers[0].getHost(), "broker.internal");
    Assert.assertEquals(servers[0].getPort(), 9094);
  }

  public void testNumericSpreadWithExplicitPortExpandsAndPreservesOrder ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("kafka-#.internal:9094");
    factory.setServerSpread("1..3");
    factory.afterPropertiesSet();

    KafkaServer[] servers = factory.getObject();

    Assert.assertNotNull(servers);
    Assert.assertEquals(servers.length, 3);
    Assert.assertEquals(servers[0].getHost(), "kafka-1.internal");
    Assert.assertEquals(servers[1].getHost(), "kafka-2.internal");
    Assert.assertEquals(servers[2].getHost(), "kafka-3.internal");
    Assert.assertEquals(servers[0].getPort(), 9094);
    Assert.assertEquals(servers[1].getPort(), 9094);
    Assert.assertEquals(servers[2].getPort(), 9094);
  }

  public void testNumericSpreadWithoutColonDefaultsToPort9092 ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("kafka-#.internal");
    factory.setServerSpread("1..3");
    factory.afterPropertiesSet();

    KafkaServer[] servers = factory.getObject();

    Assert.assertNotNull(servers);
    Assert.assertEquals(servers.length, 3);
    Assert.assertEquals(servers[0].getPort(), 9092);
    Assert.assertEquals(servers[1].getPort(), 9092);
    Assert.assertEquals(servers[2].getPort(), 9092);
  }

  public void testAlphabeticSpreadExpansion ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("kafka-#.internal:9094");
    factory.setServerSpread("a..c");
    factory.afterPropertiesSet();

    KafkaServer[] servers = factory.getObject();

    Assert.assertNotNull(servers);
    Assert.assertEquals(servers.length, 3);
    Assert.assertEquals(servers[0].getHost(), "kafka-a.internal");
    Assert.assertEquals(servers[1].getHost(), "kafka-b.internal");
    Assert.assertEquals(servers[2].getHost(), "kafka-c.internal");
  }

  @Test(expectedExceptions = NumberFormatException.class)
  public void testNonNumericPortThrowsNumberFormatException ()
    throws Exception {

    KafkaServerFactoryBean factory = new KafkaServerFactoryBean();

    factory.setServerPattern("broker.internal:abc");
    factory.afterPropertiesSet();
  }
}

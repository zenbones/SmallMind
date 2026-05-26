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
package org.smallmind.claxon.emitter.jmx;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import org.smallmind.claxon.registry.Quantity;
import org.smallmind.claxon.registry.QuantityType;
import org.smallmind.claxon.registry.Tag;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = "unit")
public class JMXEmitterTest {

  public void testRecordRegistersMbeanAndPopulatesAttributes ()
    throws Exception {

    MBeanServer server = MBeanServerFactory.newMBeanServer();
    JMXEmitter emitter = new JMXEmitter(server);
    ObjectName expected = new ObjectName("metric.requests:env=prod");

    Assert.assertFalse(server.isRegistered(expected));

    emitter.record(
      "metric.requests",
      new Tag[] {new Tag("env", "prod")},
      new Quantity[] {new Quantity("count", 4.0, QuantityType.COUNT), new Quantity("rate", 2.0)});

    Assert.assertTrue(server.isRegistered(expected));
    Assert.assertEquals(server.getAttribute(expected, "count"), 4.0);
    Assert.assertEquals(server.getAttribute(expected, "rate"), 2.0);
  }

  public void testSecondRecordUpdatesAttributesInPlace ()
    throws Exception {

    MBeanServer server = MBeanServerFactory.newMBeanServer();
    JMXEmitter emitter = new JMXEmitter(server);
    ObjectName objectName = new ObjectName("metric.updates:env=stage");

    emitter.record(
      "metric.updates",
      new Tag[] {new Tag("env", "stage")},
      new Quantity[] {new Quantity("count", 1.0, QuantityType.COUNT)});

    emitter.record(
      "metric.updates",
      new Tag[] {new Tag("env", "stage")},
      new Quantity[] {new Quantity("count", 9.0, QuantityType.COUNT)});

    Assert.assertEquals(server.getAttribute(objectName, "count"), 9.0);
    Assert.assertEquals(server.queryNames(new ObjectName("metric.updates:*"), null).size(), 1);
  }

  public void testDistinctTagsProduceDistinctMbeans ()
    throws Exception {

    MBeanServer server = MBeanServerFactory.newMBeanServer();
    JMXEmitter emitter = new JMXEmitter(server);
    Quantity[] quantities = new Quantity[] {new Quantity("count", 1.0, QuantityType.COUNT)};

    emitter.record("metric.split", new Tag[] {new Tag("zone", "a")}, quantities);
    emitter.record("metric.split", new Tag[] {new Tag("zone", "b")}, quantities);

    Assert.assertEquals(server.queryNames(new ObjectName("metric.split:*"), null).size(), 2);
  }
}

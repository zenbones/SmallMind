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
package org.smallmind.phalanx.wire.transport.jms.spring;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Locks the JMS Spring reference beans: the destination-kind discriminators (a {@link QueueReference}
 * must report {@link DestinationType#QUEUE} and a {@link TopicReference} {@link DestinationType#TOPIC})
 * and the name/path/selector/durability accessors shared via {@link ManagedObjectReference} and
 * {@link DestinationReference}.
 */
@Test(groups = "unit")
public class JmsSpringReferenceTest {

  @Test
  public void testQueueReferenceReportsQueue () {

    Assert.assertEquals(new QueueReference().getDestinationType(), DestinationType.QUEUE);
  }

  @Test
  public void testTopicReferenceReportsTopic () {

    Assert.assertEquals(new TopicReference().getDestinationType(), DestinationType.TOPIC);
  }

  @Test
  public void testDestinationReferenceAccessors () {

    QueueReference reference = new QueueReference();

    Assert.assertFalse(reference.isDurable());

    reference.setName("orders");
    reference.setPath("jms/orders");
    reference.setSelector("region = 'EU'");
    reference.setDurable(true);

    Assert.assertEquals(reference.getName(), "orders");
    Assert.assertEquals(reference.getPath(), "jms/orders");
    Assert.assertEquals(reference.getSelector(), "region = 'EU'");
    Assert.assertTrue(reference.isDurable());
  }

  @Test
  public void testConnectionFactoryReferenceAccessors () {

    ConnectionFactoryReference reference = new ConnectionFactoryReference();

    reference.setName("cf");
    reference.setPath("jms/cf");

    Assert.assertEquals(reference.getName(), "cf");
    Assert.assertEquals(reference.getPath(), "jms/cf");
  }
}

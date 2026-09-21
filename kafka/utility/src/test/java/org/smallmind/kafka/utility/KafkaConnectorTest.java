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
 * Unit tests for {@link KafkaConnector}. Covers the constructor's bootstrap-string
 * assembly contract — input ordering, duplicate handling, and the empty-broker
 * edge case both documented in the module guide. The client-construction and
 * cluster-reachability surfaces ({@code createProducer}, {@code createConsumer},
 * {@code check}, {@code invokeAdminClient}) require a broker (real or mocked at
 * the {@code AdminClient.create} static seam) and belong to the integration
 * suite.
 */
@Test(groups = "unit")
public class KafkaConnectorTest {

  public void testBootstrapStringFromSingleBroker () {

    KafkaConnector connector = new KafkaConnector(new KafkaServer("kafka-1.internal", 9094));

    Assert.assertEquals(connector.getBoostrapServers(), "kafka-1.internal:9094");
  }

  public void testBootstrapStringConcatenatesMultipleBrokersInOrder () {

    KafkaConnector connector = new KafkaConnector(
      new KafkaServer("kafka-1.internal", 9094),
      new KafkaServer("kafka-2.internal", 9095),
      new KafkaServer("kafka-3.internal", 9096));

    Assert.assertEquals(connector.getBoostrapServers(), "kafka-1.internal:9094,kafka-2.internal:9095,kafka-3.internal:9096");
  }

  public void testBootstrapStringFromZeroBrokersIsEmpty () {

    Assert.assertEquals(new KafkaConnector().getBoostrapServers(), "");
  }

  public void testBootstrapStringPreservesDuplicateBrokers () {

    KafkaServer same = new KafkaServer("kafka-1.internal", 9094);
    KafkaConnector connector = new KafkaConnector(same, same);

    Assert.assertEquals(connector.getBoostrapServers(), "kafka-1.internal:9094,kafka-1.internal:9094");
  }
}

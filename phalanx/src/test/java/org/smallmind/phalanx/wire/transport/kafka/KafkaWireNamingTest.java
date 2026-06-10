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
package org.smallmind.phalanx.wire.transport.kafka;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.TimestampType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Locks the Kafka wire-naming contract: the four topic-name formats derived by {@link TopicNames}
 * (a renamed topic silently breaks interop between client and server) and the header-lookup
 * behaviour of {@link HeaderUtility}.
 */
@Test(groups = "unit")
public class KafkaWireNamingTest {

  @Test
  public void testTopicNameFormats () {

    TopicNames topicNames = new TopicNames("wire");

    Assert.assertEquals(topicNames.getShoutTopicName("orders"), "wire-shout-orders");
    Assert.assertEquals(topicNames.getTalkTopicName("orders"), "wire-talk-orders");
    Assert.assertEquals(topicNames.getWhisperTopicName("orders", "node-7"), "wire-whisper-orders-node-7");
    Assert.assertEquals(topicNames.getResponseTopicName("caller-9"), "wire-response-caller-9");
  }

  @Test
  public void testGetHeaderReturnsMatchingValue () {

    RecordHeaders headers = new RecordHeaders();
    headers.add(HeaderUtility.MESSAGE_ID, "msg-1".getBytes(StandardCharsets.UTF_8));
    headers.add(HeaderUtility.CALLER_ID, "caller-1".getBytes(StandardCharsets.UTF_8));

    ConsumerRecord<Long, byte[]> record = new ConsumerRecord<>("wire-talk-orders", 0, 0L, 0L, TimestampType.CREATE_TIME, -1, -1, 1L, new byte[0], headers, Optional.empty());

    Assert.assertEquals(HeaderUtility.getHeader(record, HeaderUtility.MESSAGE_ID), "msg-1");
    Assert.assertEquals(HeaderUtility.getHeader(record, HeaderUtility.CALLER_ID), "caller-1");
  }

  @Test
  public void testGetHeaderReturnsNullWhenAbsent () {

    ConsumerRecord<Long, byte[]> record = new ConsumerRecord<>("wire-talk-orders", 0, 0L, 0L, TimestampType.CREATE_TIME, -1, -1, 1L, new byte[0], new RecordHeaders(), Optional.empty());

    Assert.assertNull(HeaderUtility.getHeader(record, HeaderUtility.CORRELATION_ID));
  }
}

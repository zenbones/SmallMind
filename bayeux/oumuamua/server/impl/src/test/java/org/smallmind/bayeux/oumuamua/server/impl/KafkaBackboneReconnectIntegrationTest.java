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
package org.smallmind.bayeux.oumuamua.server.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.cometd.bayeux.Message;
import org.cometd.client.BayeuxClient;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.RecordUtility;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Verifies that the {@link org.smallmind.bayeux.oumuamua.server.spi.backbone.kafka.KafkaBackbone}
 * consumer loop survives a malformed record and continues delivering subsequent valid records.
 *
 * <p>The single happy path ({@code MultiNodeKafkaFanOutIntegrationTest}) only confirms that the
 * loop functions when every record decodes cleanly. This test injects a known-bad record directly
 * onto the backbone topic via a side-channel producer, then a well-formed record originating from
 * a synthetic "remote" node, and asserts that the second record still reaches a local subscriber.
 * Together they exercise the per-record exception handler that the production code relies on to
 * keep the consumer alive in the face of corruption or out-of-band writes.
 */
@Test(groups = "integration")
public class KafkaBackboneReconnectIntegrationTest extends AbstractBayeuxIntegrationTest {

  private static final String BACKBONE_TOPIC = "oumuamua-oumuamua";
  private static final String SYNTHETIC_REMOTE_NODE = "synthetic-remote-node";
  private static final String DELIVERY_CHANNEL = "/integration/reconnect";
  private static final String PAYLOAD = "post-corruption-delivery";
  private static final long SUBSCRIBE_TIMEOUT_MILLISECONDS = 5_000L;
  private static final long CROSS_NODE_DELIVERY_TIMEOUT_MILLISECONDS = 30_000L;
  private static final long CROSS_NODE_RETRY_INTERVAL_MILLISECONDS = 1_000L;
  private static final long DISCONNECT_TIMEOUT_MILLISECONDS = 5_000L;
  private static final int KAFKA_GRACE_PERIOD_SECONDS = 10;

  private Producer<Long, byte[]> injectionProducer;

  @BeforeClass
  public void beforeClass ()
    throws Exception {

    super.beforeClass();

    KafkaConnector injectionConnector = new KafkaConnector(new KafkaServer("localhost", 9094)).check(KAFKA_GRACE_PERIOD_SECONDS);
    injectionProducer = injectionConnector.createProducer("reconnect-test-injector");
  }

  @AfterClass
  public void afterClass ()
    throws Exception {

    if (injectionProducer != null) {
      injectionProducer.close();
    }

    super.afterClass();
  }

  @Test
  public void corruptRecordIsTolerated ()
    throws Exception {

    BayeuxClient subscriber = constructBayeuxClient();

    try {
      handshakeAndAwait(subscriber);

      CountDownLatch subscribeLatch = new CountDownLatch(1);
      CountDownLatch deliveryLatch = new CountDownLatch(1);
      AtomicReference<Message> deliveredMessage = new AtomicReference<>();

      subscriber.getChannel(DELIVERY_CHANNEL).subscribe(
        (_, message) -> {
          deliveredMessage.set(message);
          deliveryLatch.countDown();
        },
        _ -> subscribeLatch.countDown());

      Assert.assertTrue(subscribeLatch.await(SUBSCRIBE_TIMEOUT_MILLISECONDS, TimeUnit.MILLISECONDS), "Subscribe acknowledgement did not arrive");

      injectionProducer.send(new ProducerRecord<>(BACKBONE_TOPIC, 0L, "this is not a valid record".getBytes())).get();

      byte[] validRecord = buildValidBackboneRecord();
      boolean delivered = false;
      long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CROSS_NODE_DELIVERY_TIMEOUT_MILLISECONDS);

      // The Kafka consumer in the backbone uses AUTO_OFFSET_RESET=latest, so the assignment
      // may take a moment after subscribe completes; retry the injection until either the
      // listener fires or the outer timeout expires. Multiple injections are safe because the
      // delivery latch only counts down once.
      while (System.nanoTime() < deadlineNanos) {
        injectionProducer.send(new ProducerRecord<>(BACKBONE_TOPIC, 0L, validRecord)).get();
        if (deliveryLatch.await(CROSS_NODE_RETRY_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS)) {
          delivered = true;
          break;
        }
      }

      Assert.assertTrue(delivered, "Valid record did not reach local subscriber after corrupt record was injected; the consumer may have died.");

      Message message = deliveredMessage.get();

      Assert.assertNotNull(message, "Captured message reference was null");
      Assert.assertEquals(message.getData(), PAYLOAD);
      Assert.assertEquals(message.getChannel(), DELIVERY_CHANNEL);
    } finally {
      disconnectAndAwait(subscriber, DISCONNECT_TIMEOUT_MILLISECONDS);
    }
  }

  /**
   * Builds a backbone-format record bytes that, when deserialized by the running backbone, yields
   * a delivery packet targeting {@link #DELIVERY_CHANNEL} with {@link #PAYLOAD} as its data. The
   * {@code nodeName} is set to a synthetic value distinct from the local node so the consumer's
   * loopback filter does not drop the record.
   */
  @SuppressWarnings("unchecked")
  private byte[] buildValidBackboneRecord ()
    throws Exception {

    OrthodoxCodec codec = new OrthodoxCodec(new JaxbDeserializer<>());
    org.smallmind.bayeux.oumuamua.server.api.json.Message<OrthodoxValue> message = codec.create();

    message.put(org.smallmind.bayeux.oumuamua.server.api.json.Message.CHANNEL, DELIVERY_CHANNEL);
    message.put(org.smallmind.bayeux.oumuamua.server.api.json.Message.DATA, codec.convert(PAYLOAD));

    Packet<OrthodoxValue> packet = new Packet<OrthodoxValue>(
      PacketType.DELIVERY,
      null,
      new DefaultRoute(DELIVERY_CHANNEL),
      new org.smallmind.bayeux.oumuamua.server.api.json.Message[] {message});

    return RecordUtility.serialize(SYNTHETIC_REMOTE_NODE, packet);
  }
}

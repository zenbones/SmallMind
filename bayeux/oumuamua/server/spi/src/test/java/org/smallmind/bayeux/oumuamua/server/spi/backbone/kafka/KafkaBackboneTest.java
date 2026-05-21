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
package org.smallmind.bayeux.oumuamua.server.spi.backbone.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.PacketType;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.json.Message;
import org.smallmind.bayeux.oumuamua.server.spi.DefaultRoute;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.RecordUtility;
import org.smallmind.bayeux.oumuamua.server.spi.json.jackson.JaxbDeserializer;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxCodec;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxMessage;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValue;
import org.smallmind.bayeux.oumuamua.server.spi.json.orthodox.OrthodoxValueFactory;
import org.smallmind.kafka.utility.KafkaConnectionException;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaConsumerType;
import org.smallmind.kafka.utility.KafkaServer;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link KafkaBackbone}. The unreachable-broker test confirms the failure-mode
 * contract of the constructor; all other tests mock the Kafka clients via
 * {@link Mockito#mockConstruction} so the publish path, the consumer worker poll loop, the
 * remote-vs.-local delivery filter, the offset-commit step, the recoverable-error reconnect
 * branch, and the wakeup-driven shutdown can be exercised without a broker. The broker-driven
 * end-to-end behavior remains covered by the integration suite in {@code oumuamua-server-impl}.
 */
@Test(groups = "unit")
public class KafkaBackboneTest {

  private static final int UNREACHABLE_PORT = 1;
  private static final int SHORT_GRACE_PERIOD_SECONDS = 1;
  private static final String NODE_NAME = "local-node";
  private static final String REMOTE_NODE = "remote-node";
  private static final String TOPIC = "test-topic";
  private static final String PREFIXED_TOPIC = "oumuamua-" + TOPIC;

  private MockedConstruction<KafkaConnector> mockedConnector;
  private Producer<Long, byte[]> producer;
  private Consumer<Long, byte[]> consumer;
  private Server<OrthodoxValue> server;
  private OrthodoxCodec codec;
  private OrthodoxValueFactory factory;

  @BeforeMethod
  @SuppressWarnings("unchecked")
  public void beforeMethod () {

    codec = new OrthodoxCodec(new JaxbDeserializer<>());
    factory = new OrthodoxValueFactory();
    producer = Mockito.mock(Producer.class);
    consumer = Mockito.mock(Consumer.class);
    server = Mockito.mock(Server.class);

    Mockito.when(server.getCodec()).thenReturn(codec);

    mockedConnector = Mockito.mockConstruction(KafkaConnector.class, (mock, ctx) -> {
      Mockito.when(mock.check(Mockito.anyInt())).thenReturn(mock);
      Mockito.when(mock.getBoostrapServers()).thenReturn("localhost:9094");
      Mockito.when(mock.createProducer(Mockito.anyString())).thenReturn(producer);
      Mockito.when(mock.createConsumer(Mockito.any(KafkaConsumerType.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.<String>any())).thenReturn(consumer);
    });
  }

  @AfterMethod
  public void afterMethod () {

    if (mockedConnector != null) {
      mockedConnector.close();
      mockedConnector = null;
    }
  }

  private KafkaBackbone<OrthodoxValue> createBackbone ()
    throws Exception {

    return new KafkaBackbone<>(NODE_NAME, 1, 5, KafkaConsumerType.CLASSIC, TOPIC, new KafkaServer("localhost", 9094));
  }

  private Packet<OrthodoxValue> makePacket ()
    throws Exception {

    Message<OrthodoxValue> message = new OrthodoxMessage(null, factory);

    message.put(Message.CHANNEL, "/foo");

    return new Packet<>(PacketType.DELIVERY, "alice", new DefaultRoute("/foo"), message);
  }

  private ConsumerRecords<Long, byte[]> recordsFromBytes (byte[] payload) {

    TopicPartition partition = new TopicPartition(PREFIXED_TOPIC, 0);
    Map<TopicPartition, List<ConsumerRecord<Long, byte[]>>> map = new HashMap<>();

    map.put(partition, Collections.singletonList(new ConsumerRecord<>(PREFIXED_TOPIC, 0, 0, 0L, payload)));

    return new ConsumerRecords<>(map);
  }

  @Test(expectedExceptions = KafkaConnectionException.class)
  public void testConstructorThrowsKafkaConnectionExceptionWhenNoBrokersReachable ()
    throws KafkaConnectionException {

    if (mockedConnector != null) {
      mockedConnector.close();
      mockedConnector = null;
    }

    new KafkaBackbone<OrthodoxValue>(
      "isolated-test-node",
      1,
      SHORT_GRACE_PERIOD_SECONDS,
      KafkaConsumerType.CLASSIC,
      "unit-test-topic",
      new KafkaServer("127.0.0.1", UNREACHABLE_PORT));
  }

  public void testConstructorOpensProducerAndChecksConnector ()
    throws Exception {

    createBackbone();

    Assert.assertEquals(mockedConnector.constructed().size(), 1);

    KafkaConnector constructedConnector = mockedConnector.constructed().get(0);

    Mockito.verify(constructedConnector).check(5);
    Mockito.verify(constructedConnector).createProducer("oumuamua-producer-" + TOPIC + "-" + NODE_NAME);
  }

  @SuppressWarnings("unchecked")
  public void testPublishSendsSerializedRecordToPrefixedTopic ()
    throws Exception {

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    backbone.publish(makePacket());

    Mockito.verify(producer, Mockito.timeout(2000).times(1)).send(Mockito.any(ProducerRecord.class));
  }

  public void testStartUpSpawnsOneConsumerPerWorker ()
    throws Exception {

    Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
      Thread.sleep(50);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);

      KafkaConnector constructedConnector = mockedConnector.constructed().get(0);

      Mockito.verify(constructedConnector, Mockito.timeout(2000).times(1))
               .createConsumer(Mockito.any(KafkaConsumerType.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.<String>any());
    } finally {
      backbone.shutDown();
    }
  }

  @SuppressWarnings("unchecked")
  public void testWorkerDeliversRemoteRecordAndSkipsLocal ()
    throws Exception {

    CountDownLatch delivered = new CountDownLatch(1);

    Mockito.doAnswer(invocation -> {
      delivered.countDown();
      return null;
    }).when(server).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));

    TopicPartition partition = new TopicPartition(PREFIXED_TOPIC, 0);
    List<ConsumerRecord<Long, byte[]>> list = new LinkedList<>();

    list.add(new ConsumerRecord<>(PREFIXED_TOPIC, 0, 0, 0L, RecordUtility.serialize(REMOTE_NODE, makePacket())));
    list.add(new ConsumerRecord<>(PREFIXED_TOPIC, 0, 1, 0L, RecordUtility.serialize(NODE_NAME, makePacket())));

    Map<TopicPartition, List<ConsumerRecord<Long, byte[]>>> map = new HashMap<>();

    map.put(partition, list);

    ConsumerRecords<Long, byte[]> batch = new ConsumerRecords<>(map);

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(batch).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);

      Assert.assertTrue(delivered.await(3, TimeUnit.SECONDS), "Expected remote packet to be delivered");
      Mockito.verify(server, Mockito.times(1)).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));
      Mockito.verify(consumer, Mockito.timeout(2000)).commitSync(Mockito.<Map<TopicPartition, OffsetAndMetadata>>any());
    } finally {
      backbone.shutDown();
    }
  }

  public void testWorkerSwallowsDeserializeErrorAndCommitsBatch ()
    throws Exception {

    ConsumerRecords<Long, byte[]> bad = recordsFromBytes(new byte[] {0, 0, 0, 1, 1, 0, 0, 0, 1, 2, 0, 0, 0, 1, 3});

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(bad).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);

      Mockito.verify(consumer, Mockito.timeout(2000)).commitSync(Mockito.<Map<TopicPartition, OffsetAndMetadata>>any());
      Mockito.verify(server, Mockito.never()).deliver(Mockito.any(), Mockito.any(Packet.class), Mockito.anyBoolean());
    } finally {
      backbone.shutDown();
    }
  }

  public void testWorkerRecreatesConsumerAfterPollException ()
    throws Exception {

    Mockito.when(consumer.poll(Mockito.any()))
             .thenThrow(new RuntimeException("simulated transient failure"))
             .thenAnswer(invocation -> {
               Thread.sleep(100);
               return ConsumerRecords.empty();
             });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);

      KafkaConnector constructedConnector = mockedConnector.constructed().get(0);

      Mockito.verify(constructedConnector, Mockito.timeout(2000).times(2))
               .createConsumer(Mockito.any(KafkaConsumerType.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.<String>any());
      Mockito.verify(consumer, Mockito.atLeastOnce()).unsubscribe();
      Mockito.verify(consumer, Mockito.atLeastOnce()).close();
    } finally {
      backbone.shutDown();
    }
  }

  public void testShutDownWakesUpWorkerAndClosesConsumer ()
    throws Exception {

    CountDownLatch entered = new CountDownLatch(1);

    Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
      entered.countDown();
      Thread.sleep(500);
      throw new WakeupException();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    backbone.startUp(server);
    Assert.assertTrue(entered.await(2, TimeUnit.SECONDS), "Worker should have entered poll");
    backbone.shutDown();

    Mockito.verify(consumer, Mockito.atLeastOnce()).wakeup();
    Mockito.verify(consumer, Mockito.atLeastOnce()).close();
  }

  public void testIdempotentStartUpDoesNotSpawnExtraWorkers ()
    throws Exception {

    Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);
      backbone.startUp(server);

      KafkaConnector constructedConnector = mockedConnector.constructed().get(0);

      Mockito.verify(constructedConnector, Mockito.times(1))
               .createConsumer(Mockito.any(KafkaConsumerType.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.<String>any());
    } finally {
      backbone.shutDown();
    }
  }

  public void testIdempotentShutDownDoesNotWakeWorkerTwice ()
    throws Exception {

    Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    backbone.startUp(server);
    backbone.shutDown();
    backbone.shutDown();

    Mockito.verify(consumer, Mockito.times(1)).wakeup();
  }
}

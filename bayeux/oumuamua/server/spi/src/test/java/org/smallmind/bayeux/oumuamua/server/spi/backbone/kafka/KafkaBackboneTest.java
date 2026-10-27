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

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.mockito.ArgumentCaptor;
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
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.nutsnbolts.context.PerApplicationContext;
import org.smallmind.nutsnbolts.lang.PerApplicationDataManager;
import org.smallmind.nutsnbolts.scope.MissingPerApplicationScopeException;
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.smallmind.nutsnbolts.util.ComponentStateException;
import org.smallmind.scribe.pen.Level;
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

  private static void awaitRecords (CapturingAppender appender, int count, long timeoutMillis)
    throws InterruptedException {

    long deadline = System.currentTimeMillis() + timeoutMillis;

    while ((appender.size() < count) && (System.currentTimeMillis() < deadline)) {
      Thread.sleep(20);
    }
  }

  private static Object probeScope () {

    try {

      return PerApplicationScope.getPerApplicationData(SampleManager.class, String.class);
    } catch (MissingPerApplicationScopeException missingPerApplicationScopeException) {

      return missingPerApplicationScopeException;
    }
  }

  @BeforeMethod
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
      Mockito.when(mock.createConsumer(Mockito.any(KafkaGroupProtocol.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.<String>any())).thenReturn(consumer);
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

    return createBackbone(null);
  }

  private KafkaBackbone<OrthodoxValue> createBackbone (PerApplicationScope perApplicationScope)
    throws Exception {

    return createBackbone(1, perApplicationScope);
  }

  private KafkaBackbone<OrthodoxValue> createBackbone (int concurrencyLimit, PerApplicationScope perApplicationScope)
    throws Exception {

    return new KafkaBackbone<>(NODE_NAME, concurrencyLimit, 5, KafkaGroupProtocol.CLASSIC, true, TOPIC, perApplicationScope, new KafkaServer("localhost", 9094));
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
      KafkaGroupProtocol.CLASSIC,
      true,
      "unit-test-topic",
      null,
      new KafkaServer("127.0.0.1", UNREACHABLE_PORT));
  }

  public void testConstructorOpensProducerAndChecksConnector ()
    throws Exception {

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      Assert.assertEquals(mockedConnector.constructed().size(), 1);

      KafkaConnector constructedConnector = mockedConnector.constructed().get(0);

      Mockito.verify(constructedConnector).check(5);
      Mockito.verify(constructedConnector).createProducer("oumuamua-producer-" + TOPIC + "-" + NODE_NAME);
    } finally {
      backbone.shutDown();
    }
  }

  public void testPublishSendsSerializedRecordToPrefixedTopic ()
    throws Exception {

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {

      ArgumentCaptor<ProducerRecord<Long, byte[]>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);

      backbone.publish(makePacket());

      Mockito.verify(producer, Mockito.timeout(2000).times(1)).send(recordCaptor.capture(), Mockito.any(Callback.class));
      Assert.assertEquals(recordCaptor.getValue().topic(), PREFIXED_TOPIC);
    } finally {
      backbone.shutDown();
    }
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
        .createConsumer(Mockito.any(KafkaGroupProtocol.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.<String>any());
    } finally {
      backbone.shutDown();
    }
  }

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

  // The consumer worker's whole poll loop runs inside the backbone's scope, so the server sees that scope on delivery.
  public void testWorkerDeliversInsideTheGivenScope ()
    throws Exception {

    PerApplicationScope perApplicationScope = new PerApplicationScope();
    AtomicReference<Object> seenOnDeliver = new AtomicReference<>();
    CountDownLatch delivered = new CountDownLatch(1);

    perApplicationScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Mockito.doAnswer(invocation -> {
      seenOnDeliver.set(probeScope());
      delivered.countDown();
      return null;
    }).when(server).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(recordsFromBytes(RecordUtility.serialize(REMOTE_NODE, makePacket()))).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone(perApplicationScope);

    try {
      backbone.startUp(server);

      Assert.assertTrue(delivered.await(3, TimeUnit.SECONDS), "Expected remote packet to be delivered");
      Assert.assertEquals(seenOnDeliver.get(), "scoped", "the consumer worker must run inside the backbone's scope");
    } finally {
      backbone.shutDown();
    }
  }

  public void testWorkerDeliversUnboundWithoutAScope ()
    throws Exception {

    AtomicReference<Object> seenOnDeliver = new AtomicReference<>();
    CountDownLatch delivered = new CountDownLatch(1);

    Mockito.doAnswer(invocation -> {
      seenOnDeliver.set(probeScope());
      delivered.countDown();
      return null;
    }).when(server).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(recordsFromBytes(RecordUtility.serialize(REMOTE_NODE, makePacket()))).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    try {
      backbone.startUp(server);

      Assert.assertTrue(delivered.await(3, TimeUnit.SECONDS), "Expected remote packet to be delivered");
      Assert.assertTrue(seenOnDeliver.get() instanceof MissingPerApplicationScopeException, "a backbone given no scope on an unbound thread must leave its worker unbound, saw " + seenOnDeliver.get());
    } finally {
      backbone.shutDown();
    }
  }

  // A backbone given no scope adopts the one bound on its constructing thread, so the worker runs inside that scope
  // even though startUp is later called from an unbound thread.
  public void testWorkerDeliversInsideTheScopeBoundAtConstructionWhenGivenNone ()
    throws Exception {

    PerApplicationScope boundScope = new PerApplicationScope();
    AtomicReference<Object> seenOnDeliver = new AtomicReference<>();
    CountDownLatch delivered = new CountDownLatch(1);

    boundScope.run(() -> PerApplicationScope.setPerApplicationData(SampleManager.class, "scoped"));

    Mockito.doAnswer(invocation -> {
      seenOnDeliver.set(probeScope());
      delivered.countDown();
      return null;
    }).when(server).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(recordsFromBytes(RecordUtility.serialize(REMOTE_NODE, makePacket()))).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = boundScope.call(() -> createBackbone(null));

    try {
      backbone.startUp(server);

      Assert.assertTrue(delivered.await(3, TimeUnit.SECONDS), "Expected remote packet to be delivered");
      Assert.assertEquals(seenOnDeliver.get(), "scoped", "a backbone given no scope must adopt the scope bound on its constructing thread");
    } finally {
      backbone.shutDown();
    }
  }

  // A scope handed in while a different scope is bound on the constructing thread is rejected before any Kafka
  // resource is opened.
  public void testConstructorRejectsAScopeThatDiffersFromTheBoundOne ()
    throws Exception {

    PerApplicationScope boundScope = new PerApplicationScope();
    PerApplicationScope handedScope = new PerApplicationScope();

    Assert.assertThrows(ScopeException.class, () -> boundScope.call(() -> createBackbone(handedScope)));
    Assert.assertEquals(mockedConnector.constructed().size(), 0, "the connector must not be opened when the scope is rejected");
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
        .createConsumer(Mockito.any(KafkaGroupProtocol.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.<String>any());
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
        .createConsumer(Mockito.any(KafkaGroupProtocol.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.<String>any());
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

  // A backbone given no scope starts its worker as a plain thread, which inherits whatever context the thread calling
  // startUp() carries; the backbone itself is built on the test thread, where the construction mock is in force. A
  // value written through the startUp() thread's map before the start is read back on the worker, so it is the same
  // map and not merely some context.
  public void testWorkerInheritsTheStartUpCallersContextWithoutAScope ()
    throws Exception {

    AtomicReference<Boolean> contextBoundOnDeliver = new AtomicReference<>();
    AtomicReference<Object> seenOnDeliver = new AtomicReference<>();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch delivered = new CountDownLatch(1);
    KafkaBackbone<OrthodoxValue> backbone;
    Thread thread;

    Mockito.doAnswer(invocation -> {
      contextBoundOnDeliver.set(PerApplicationContext.isBound());
      if (PerApplicationContext.isBound()) {
        seenOnDeliver.set(PerApplicationContext.getPerApplicationData(SampleManager.class, String.class));
      }
      delivered.countDown();
      return null;
    }).when(server).deliver(Mockito.isNull(), Mockito.any(Packet.class), Mockito.eq(false));

    Mockito.when(consumer.poll(Mockito.any())).thenReturn(recordsFromBytes(RecordUtility.serialize(REMOTE_NODE, makePacket()))).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    backbone = createBackbone();
    thread = Thread.ofPlatform().name("isolated-probe").daemon(false).inheritInheritableThreadLocals(false).unstarted(() -> {
      try {
        new PerApplicationContext();
        PerApplicationContext.setPerApplicationData(SampleManager.class, "startup-thread-map");
        backbone.startUp(server);
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    thread.start();
    thread.join();

    try {
      Assert.assertNull(failure.get(), "startUp must succeed on the context-carrying thread, saw " + failure.get());
      Assert.assertTrue(delivered.await(3, TimeUnit.SECONDS), "Expected remote packet to be delivered");
      Assert.assertEquals(contextBoundOnDeliver.get(), Boolean.TRUE, "the consumer worker must inherit the context of the thread that called startUp()");
      Assert.assertEquals(seenOnDeliver.get(), "startup-thread-map", "the consumer worker must carry the very map attached to the thread that called startUp()");
    } finally {
      backbone.shutDown();
    }
  }

  // Once stopped the backbone is terminal: the send executor is shut down and the producer is closed, so a later
  // publish is dropped with a warning rather than sent or thrown.
  public void testShutDownClosesTheProducerAndDropsLaterPublishesWithAWarning ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(KafkaBackbone.class, Level.WARN);

    try {
      Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
        Thread.sleep(100);
        return ConsumerRecords.empty();
      });

      KafkaBackbone<OrthodoxValue> backbone = createBackbone();

      backbone.startUp(server);
      backbone.shutDown();

      Mockito.verify(producer, Mockito.times(1)).close(Mockito.any(Duration.class));
      Assert.assertEquals(appender.size(), 0, "a shutdown with nothing queued is expected to log no warning, saw " + appender.getRecords());

      backbone.publish(makePacket());

      Assert.assertEquals(appender.size(), 1, "a publish after shutdown is expected to warn exactly once, saw " + appender.getRecords());
      Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.WARN);
      Assert.assertTrue(appender.getRecords().get(0).getMessage().contains("has been shut down"), appender.getRecords().get(0).getMessage());
      Mockito.verify(producer, Mockito.never()).send(Mockito.any(ProducerRecord.class), Mockito.any(Callback.class));
    } finally {
      CapturingAppender.release(KafkaBackbone.class);
    }
  }

  // The send runs on the executor, so a producer failure is logged at ERROR there and never reaches the publisher.
  public void testPublishLogsAtErrorWhenTheProducerThrows ()
    throws Exception {

    CapturingAppender appender = CapturingAppender.attach(KafkaBackbone.class, Level.ERROR);
    RuntimeException sendFailure = new RuntimeException("simulated send failure");

    try {
      Mockito.when(producer.send(Mockito.any(ProducerRecord.class), Mockito.any(Callback.class))).thenThrow(sendFailure);

      KafkaBackbone<OrthodoxValue> backbone = createBackbone();

      try {
        backbone.publish(makePacket());

        Mockito.verify(producer, Mockito.timeout(2000).times(1)).send(Mockito.any(ProducerRecord.class), Mockito.any(Callback.class));
        awaitRecords(appender, 1, 2000);

        Assert.assertEquals(appender.size(), 1, "the send failure is expected to be logged exactly once, saw " + appender.getRecords());
        Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.ERROR);
        Assert.assertSame(appender.getRecords().get(0).getThrown(), sendFailure, "the logged record must carry the producer's exception");
      } finally {
        backbone.shutDown();
      }
    } finally {
      CapturingAppender.release(KafkaBackbone.class);
    }
  }

  // The executor task logs after the mocked send returns, so the record can land a moment after the send is verified.
  // Failures Kafka reports through the send callback, not by throwing, must be logged too.
  @Test
  public void testPublishLogsAtErrorWhenTheProducerReportsAnAsynchronousFailure ()
    throws Exception {

    RuntimeException asyncFailure = new RuntimeException("broker unavailable");
    CapturingAppender appender = CapturingAppender.attach(KafkaBackbone.class, Level.ERROR);

    try {
      KafkaBackbone<OrthodoxValue> backbone = createBackbone();

      try {
        Mockito.when(producer.send(Mockito.any(ProducerRecord.class), Mockito.any(Callback.class))).thenAnswer((invocation) -> {
          ((Callback)invocation.getArgument(1)).onCompletion(null, asyncFailure);

          return null;
        });

        backbone.publish(makePacket());
        awaitRecords(appender, 1, 2000);

        Assert.assertEquals(appender.size(), 1, "expected one ERROR record, saw " + appender.getRecords());
        Assert.assertEquals(appender.getRecords().get(0).getLevel(), Level.ERROR);
        Assert.assertSame(appender.getRecords().get(0).getThrown(), asyncFailure);
      } finally {
        backbone.shutDown();
      }
    } finally {
      CapturingAppender.release(KafkaBackbone.class);
    }
  }

  // After shutDown the backbone is terminal; a second startUp is refused rather than quietly starting consumers
  // against a closed producer.
  @Test
  public void testStartUpAfterShutDownIsRefused ()
    throws Exception {

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    backbone.shutDown();

    try {
      backbone.startUp(server);
      Assert.fail("startUp after shutDown must throw ComponentStateException");
    } catch (ComponentStateException componentStateException) {
      Assert.assertTrue(componentStateException.getMessage().contains("shut down"), componentStateException.getMessage());
    }
  }

  // An interrupt during the wait for one worker must not strand the workers behind it: every worker is signalled
  // before any is waited for, so all of them still exit, and the producer is still closed.
  @Test
  public void testInterruptedShutDownSignalsEveryWorkerAndReleasesResources ()
    throws Exception {

    Mockito.when(consumer.poll(Mockito.any())).thenAnswer(invocation -> {
      Thread.sleep(100);
      return ConsumerRecords.empty();
    });

    KafkaBackbone<OrthodoxValue> backbone = createBackbone(2, null);

    backbone.startUp(server);
    Mockito.verify(consumer, Mockito.timeout(2000).atLeast(2)).poll(Mockito.any());

    Thread.currentThread().interrupt();
    try {
      backbone.shutDown();
      Assert.fail("shutDown on an interrupted thread must propagate the interrupt");
    } catch (InterruptedException interruptedException) {
      Assert.assertFalse(Thread.currentThread().isInterrupted(), "the wait consumed the interrupt flag");
    }

    Mockito.verify(producer, Mockito.times(1)).close(Mockito.any(Duration.class));
    // both workers share the one mocked consumer, and each closes it on the way out even though neither was waited for
    Mockito.verify(consumer, Mockito.timeout(2000).times(2)).close();
  }

  // A backbone that was constructed but never started still owns a producer; shutDown releases it.
  @Test
  public void testShutDownOnANeverStartedBackboneClosesTheProducer ()
    throws Exception {

    KafkaBackbone<OrthodoxValue> backbone = createBackbone();

    backbone.shutDown();

    Mockito.verify(producer, Mockito.times(1)).close(Mockito.any(Duration.class));
  }

  public static class SampleManager implements PerApplicationDataManager {

  }
}

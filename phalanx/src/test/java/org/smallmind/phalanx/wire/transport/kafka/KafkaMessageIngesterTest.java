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

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Drives the worker thread inside {@link KafkaMessageIngester} against a hand-built fake consumer (no
 * broker) to exercise the lifecycle branches the broker-backed integration tests cannot force on
 * demand: a poll failure must close and recreate the consumer rather than kill the worker, and a
 * paused worker must idle instead of polling an unsubscribed consumer while still resuming on play.
 */
@Test(groups = "unit")
public class KafkaMessageIngesterTest {

  private static Consumer<Long, byte[]> proxyConsumer (AtomicInteger pollCount, boolean failOnPoll) {

    return (Consumer<Long, byte[]>)Proxy.newProxyInstance(KafkaMessageIngesterTest.class.getClassLoader(), new Class[] {Consumer.class}, (proxy, method, args) -> {

      if (method.getName().equals("poll")) {
        pollCount.incrementAndGet();

        if (failOnPoll) {
          Thread.sleep(50);

          throw new RuntimeException("induced poll failure");
        }

        return ConsumerRecords.<Long, byte[]>empty();
      }

      if (method.getName().equals("assignment")) {
        //  A real KafkaConsumer returns an empty set (never null) before partitions are assigned.
        return Collections.emptySet();
      }

      return null;
    });
  }

  private static Consumer<Long, byte[]> assignedProxyConsumer () {

    return (Consumer<Long, byte[]>)Proxy.newProxyInstance(KafkaMessageIngesterTest.class.getClassLoader(), new Class[] {Consumer.class}, (proxy, method, args) -> switch (method.getName()) {
      case "poll" -> ConsumerRecords.<Long, byte[]>empty();
      case "assignment" -> Collections.singleton(new TopicPartition("test-topic", 0));
      case "position" -> 0L;
      default -> null;
    });
  }

  @Test
  public void testAwaitConsumerAssignmentReturnsTrueOnceAssignedAndPositioned ()
    throws InterruptedException {

    KafkaConnector connector = new KafkaConnector(new KafkaServer("localhost", 9092)) {

      @Override
      public Consumer<Long, byte[]> createConsumer (KafkaGroupProtocol groupProtocol, String instanceId, String clientId, String groupId, String... topics) {

        return assignedProxyConsumer();
      }
    };

    KafkaMessageIngester ingester = new KafkaMessageIngester("test-node", "test-group", "test-topic", connector, KafkaGroupProtocol.CONSUMER, record -> {
    }, 1).startUp();

    try {
      Assert.assertTrue(ingester.awaitConsumerAssignment(5, TimeUnit.SECONDS), "a consumer reporting a non-empty assignment should satisfy the readiness wait");
    } finally {
      ingester.shutDown();
    }
  }

  @Test
  public void testAwaitConsumerAssignmentTimesOutWhenNeverAssigned ()
    throws InterruptedException {

    AtomicInteger pollCount = new AtomicInteger(0);
    KafkaConnector connector = new KafkaConnector(new KafkaServer("localhost", 9092)) {

      @Override
      public Consumer<Long, byte[]> createConsumer (KafkaGroupProtocol groupProtocol, String instanceId, String clientId, String groupId, String... topics) {

        return proxyConsumer(pollCount, false);
      }
    };

    KafkaMessageIngester ingester = new KafkaMessageIngester("test-node", "test-group", "test-topic", connector, KafkaGroupProtocol.CONSUMER, record -> {
    }, 1).startUp();

    try {
      //  The consumer never reports an assignment, so the wait must time out and return false — this is
      //  exactly the signal the transport constructors turn into a KafkaConnectionException rather than
      //  starting up silently unready.
      Assert.assertFalse(ingester.awaitConsumerAssignment(500, TimeUnit.MILLISECONDS), "an unassigned consumer must not be reported ready");
      Assert.assertFalse(ingester.awaitConsumerAssignment(0, TimeUnit.SECONDS), "a non-positive timeout must return false immediately");
    } finally {
      ingester.shutDown();
    }
  }

  @Test
  public void testRecreatesConsumerAfterPollFailure ()
    throws InterruptedException {

    AtomicInteger pollCount = new AtomicInteger(0);
    AtomicInteger createdConsumers = new AtomicInteger(0);
    KafkaConnector connector = new KafkaConnector(new KafkaServer("localhost", 9092)) {

      @Override
      public Consumer<Long, byte[]> createConsumer (KafkaGroupProtocol groupProtocol, String instanceId, String clientId, String groupId, String... topics) {

        createdConsumers.incrementAndGet();

        return proxyConsumer(pollCount, true);
      }
    };

    KafkaMessageIngester ingester = new KafkaMessageIngester("test-node", "test-group", "test-topic", connector, KafkaGroupProtocol.CONSUMER, record -> {
    }, 1).startUp();

    try {
      Thread.sleep(500);

      Assert.assertTrue(createdConsumers.get() >= 2, "a failing poll should close and recreate the consumer; created=" + createdConsumers.get());
    } finally {
      ingester.shutDown();
    }
  }

  @Test
  public void testPausedWorkerStopsPollingAndResumesOnPlay ()
    throws InterruptedException {

    AtomicInteger pollCount = new AtomicInteger(0);
    KafkaConnector connector = new KafkaConnector(new KafkaServer("localhost", 9092)) {

      @Override
      public Consumer<Long, byte[]> createConsumer (KafkaGroupProtocol groupProtocol, String instanceId, String clientId, String groupId, String... topics) {

        return proxyConsumer(pollCount, false);
      }
    };

    KafkaMessageIngester ingester = new KafkaMessageIngester("test-node", "test-group", "test-topic", connector, KafkaGroupProtocol.CONSUMER, record -> {
    }, 1).startUp();

    try {
      Thread.sleep(500);
      Assert.assertTrue(pollCount.get() > 0, "a playing worker should poll its consumer");

      ingester.pause();
      Thread.sleep(1500);

      int afterPause = pollCount.get();
      Thread.sleep(1500);

      Assert.assertEquals(pollCount.get(), afterPause, "a paused worker must not poll its unsubscribed consumer");

      ingester.play();
      Thread.sleep(1500);

      Assert.assertTrue(pollCount.get() > afterPause, "a resumed worker should poll again after play");
    } finally {
      ingester.shutDown();
    }
  }
}

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

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.DebonedPacket;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.RecordUtility;
import org.smallmind.kafka.utility.KafkaConnectionException;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.nutsnbolts.util.ComponentModulator;
import org.smallmind.nutsnbolts.util.ComponentStateException;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Kafka-backed {@link Backbone} that fans every published packet out to all nodes in the
 * Oumuamua cluster by writing to a shared topic prefixed with {@code oumuamua-}.
 *
 * <p>Each node creates a unique consumer group at startup (via a Snowflake-generated group ID)
 * so that every node independently receives every record.  Records produced by the local node
 * are skipped on consumption to prevent loopback delivery.  A pool of virtual-thread consumer
 * workers polls the topic, deserializes each record with {@link RecordUtility}, and delivers
 * the packet to the local server.  Workers automatically recreate their consumers on recoverable
 * poll errors.
 *
 * @param <V> the concrete {@link Value} type carried in Bayeux messages
 */
public class KafkaBackbone<V extends Value<V>> implements Backbone<V> {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private final ComponentModulator componentModulator = new ComponentModulator();
  private final KafkaConnector connector;
  private final Producer<Long, byte[]> producer;
  private final KafkaGroupProtocol groupProtocol;
  private final String nodeName;
  private final String topicName;
  private final String prefixedTopicName;
  private final String groupId;
  private final int concurrencyLimit;
  private ConsumerWorker<V>[] workers;

  /**
   * Creates the backbone, verifies broker availability, and opens a shared producer.
   *
   * @param nodeName                  unique name for this cluster node; embedded in every produced record
   *                                  and used to skip locally-originating records on consumption
   * @param concurrencyLimit          number of parallel consumer worker threads spawned at {@link #startUp}
   * @param startupGracePeriodSeconds maximum seconds to wait for at least one broker to become reachable
   * @param groupProtocol             Kafka group protocol for the backbone's consumer workers
   * @param topicName                 logical topic name; the actual Kafka topic is {@code oumuamua-<topicName>}
   * @param servers                   one or more Kafka bootstrap broker addresses
   * @throws KafkaConnectionException if no broker is reachable within the startup grace period
   */
  public KafkaBackbone (String nodeName, int concurrencyLimit, int startupGracePeriodSeconds, KafkaGroupProtocol groupProtocol, String topicName, KafkaServer... servers)
    throws KafkaConnectionException {

    this.nodeName = nodeName;
    this.concurrencyLimit = concurrencyLimit;
    this.groupProtocol = groupProtocol;
    this.topicName = topicName;

    groupId = SnowflakeId.newInstance().generateHexEncoding();

    LoggerManager.getLogger(KafkaBackbone.class).info("Starting Kafka backbone...");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);
    LoggerManager.getLogger(KafkaBackbone.class).info("Started Kafka backbone with bootstrap servers(%s)...", connector.getBoostrapServers());

    prefixedTopicName = "oumuamua-" + topicName;
    producer = connector.createProducer("oumuamua-producer-" + topicName + "-" + nodeName);
  }

  /**
   * Creates and subscribes a new Kafka consumer for the worker at {@code index}.
   * Each worker uses the same group ID so the full fan-out is preserved.
   *
   * @param index zero-based worker index used to form a unique consumer client ID
   * @return a new {@link Consumer} already subscribed to the backbone topic
   */
  private Consumer<Long, byte[]> createConsumer (int index) {

    return connector.createConsumer(groupProtocol, nodeName, "oumuamua-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, prefixedTopicName);
  }

  /**
   * Spawns {@code concurrencyLimit} consumer worker threads and transitions the backbone to
   * {@link ComponentStatus#STARTED}.  Blocks if a concurrent state transition is in progress.
   *
   * @param server server to which deserialized packets from remote nodes will be delivered
   * @throws ComponentStateException if the backbone cannot reach the started state
   * @throws InterruptedException    if interrupted while waiting for the state transition
   */
  @Override
  public void startUp (Server<V> server)
    throws ComponentStateException, InterruptedException {

    if (componentModulator.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      workers = new ConsumerWorker[concurrencyLimit];

      for (int index = 0; index < concurrencyLimit; index++) {
        new Thread(workers[index] = new ConsumerWorker<V>(server, nodeName, index)).start();
      }
      componentModulator.set(ComponentStatus.STARTED);
    } else if (ComponentStatus.STOPPED.equals(componentModulator.awaitIn(ComponentStatus.STOPPED, ComponentStatus.STARTED))) {
      throw new ComponentStateException("Could not enter the started state");
    }
  }

  /**
   * Wakes up all consumer workers, waits for each to exit, and transitions the backbone to
   * {@link ComponentStatus#STOPPED}.  Blocks if a concurrent state transition is in progress.
   *
   * @throws ComponentStateException if the backbone cannot reach the stopped state
   * @throws InterruptedException    if interrupted while waiting for worker exit or the state transition
   */
  @Override
  public void shutDown ()
    throws ComponentStateException, InterruptedException {

    if (componentModulator.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      for (ConsumerWorker<V> worker : workers) {
        worker.stop();
      }
      componentModulator.set(ComponentStatus.STOPPED);
    } else if (ComponentStatus.STARTED.equals(componentModulator.awaitIn(ComponentStatus.STOPPED, ComponentStatus.STARTED))) {
      throw new ComponentStateException("Could not enter the stopped state");
    }
  }

  /**
   * Serializes {@code packet} and publishes it to the backbone topic via the virtual-thread executor.
   * Serialization and send errors are logged but not propagated; this is a best-effort fan-out.
   *
   * @param packet packet to distribute to all cluster nodes
   */
  @Override
  public void publish (Packet<V> packet) {

    executorService.submit(() -> {
      try {
        producer.send(new ProducerRecord<>(prefixedTopicName, RecordUtility.serialize(nodeName, packet)));
      } catch (IOException ioException) {
        LoggerManager.getLogger(KafkaBackbone.class).error(ioException);
      }
    });
  }

  /**
   * Single-threaded consumer that polls the backbone topic, deserializes each record, delivers
   * packets from remote nodes to the local server, commits offsets per partition, and replaces
   * its consumer automatically on recoverable errors.
   */
  private class ConsumerWorker<V extends Value<V>> implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Server<V> server;
    private final String nodeName;
    private final int index;
    private Consumer<Long, byte[]> consumer;

    /**
     * Creates a consumer worker for the given server and slot.
     *
     * @param server   local server to which packets from remote nodes are delivered
     * @param nodeName name of this cluster node; records whose node name matches are skipped
     * @param index    zero-based worker index used when recreating the consumer after an error
     */
    public ConsumerWorker (Server<V> server, String nodeName, int index) {

      this.server = server;
      this.nodeName = nodeName;
      this.index = index;

      consumer = createConsumer(index);
    }

    /**
     * Signals the poll loop to stop and blocks until the worker thread has fully exited.
     *
     * @throws InterruptedException if interrupted while waiting on the exit latch
     */
    private void stop ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();

        exitLatch.await();
      }
    }

    private synchronized void recreateConsumer () {

      try {
        consumer.unsubscribe();
        consumer.close();
      } finally {
        consumer = createConsumer(index);
      }
    }

    /**
     * Polls the backbone topic in a loop.  For each record, deserializes the packet and — if
     * the producing node differs from the local node — delivers it to the server.  Offsets are
     * committed synchronously per partition after each batch.  On recoverable poll errors the
     * consumer is replaced before the next iteration.  A {@link WakeupException} from a
     * {@link #stop()} call exits the loop cleanly; unexpected wakeups are logged as errors.
     */
    @Override
    public void run () {

      try {
        while (!finished.get()) {
          try {

            ConsumerRecords<Long, byte[]> records;

            if (((records = consumer.poll(Duration.ofSeconds(3))) != null) && (!records.isEmpty())) {
              for (TopicPartition partition : records.partitions()) {

                List<ConsumerRecord<Long, byte[]>> recordList;
                long lastOffset = 0;

                for (ConsumerRecord<Long, byte[]> record : recordList = records.records(partition)) {
                  try {

                    DebonedPacket<V> debonedPacket = RecordUtility.deserialize(server.getCodec(), record.value());

                    if (!nodeName.equals(debonedPacket.getNodeName())) {
                      server.deliver(null, debonedPacket.getPacket(), false);
                    }
                  } catch (Exception exception) {
                    LoggerManager.getLogger(KafkaBackbone.class).error(exception);
                  }

                  lastOffset = record.offset();
                }

                if (!recordList.isEmpty()) {
                  consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                }
              }
            }
          } catch (WakeupException wakeupException) {
            if (!finished.get()) {
              LoggerManager.getLogger(KafkaBackbone.class).error(wakeupException);
              recreateConsumer();
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(KafkaBackbone.class).error(exception);
            recreateConsumer();
          }
        }
      } finally {
        try {
          consumer.unsubscribe();
          consumer.close();
        } finally {
          exitLatch.countDown();
        }
      }
    }
  }
}

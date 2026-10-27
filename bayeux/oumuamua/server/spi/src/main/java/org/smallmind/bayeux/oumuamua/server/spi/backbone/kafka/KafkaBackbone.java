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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
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
import org.smallmind.nutsnbolts.scope.PerApplicationScope;
import org.smallmind.nutsnbolts.scope.ScopeException;
import org.smallmind.nutsnbolts.util.ComponentModulator;
import org.smallmind.nutsnbolts.util.ComponentStateException;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Kafka-backed {@link Backbone} that fans every published packet out to all nodes in the
 * Oumuamua cluster by writing to a shared topic prefixed with {@code oumuamua-}.
 *
 * <p>Each node is given a unique consumer group when constructed (via a Snowflake-generated group ID)
 * so that every node independently receives every record.  Records produced by the local node
 * are skipped on consumption to prevent loopback delivery.  A pool of consumer workers, each on a
 * platform thread of its own (not a virtual thread), polls the topic, deserializes each record with
 * {@link RecordUtility}, and delivers
 * the packet to the local server.  Workers automatically recreate their consumers on recoverable
 * poll errors.
 *
 * <p>Consumer worker threads inherit no {@code ScopedValue} binding, but they do inherit the
 * {@code PerApplicationContext} of the thread that calls {@link #startUp}. The backbone resolves its
 * {@link PerApplicationScope} once, in the constructor, through {@link PerApplicationScope#resolve}: a scope handed
 * in is used, and a {@code null} adopts whichever scope is bound on the constructing thread. When that leaves it
 * with a scope, each worker's whole poll loop, and so every packet it delivers to the local server, runs inside it;
 * when it leaves none, the workers start with no scope, carrying only that inherited context. The producer's
 * executor only calls {@code producer.send} and is left unwrapped.
 *
 * @param <V> the concrete {@link Value} type carried in Bayeux messages
 */
public class KafkaBackbone<V extends Value<V>> implements Backbone<V> {

  private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
  private final ComponentModulator componentModulator = new ComponentModulator();
  private final KafkaConnector connector;
  private final Producer<Long, byte[]> producer;
  private final KafkaGroupProtocol groupProtocol;
  private final PerApplicationScope perApplicationScope;
  private final String nodeName;
  private final String topicName;
  private final String prefixedTopicName;
  private final int startupGracePeriodSeconds;
  private final AtomicBoolean resourcesReleased = new AtomicBoolean(false);
  private final String groupId;
  private final boolean dynamicConsumers;
  private final int concurrencyLimit;
  private ConsumerWorker<V>[] workers;

  /**
   * Creates the backbone, verifies broker availability, and opens a shared producer.
   *
   * @param nodeName                  unique name for this cluster node; embedded in every produced record and
   *                                  used to skip locally-originating records on consumption. When
   *                                  {@code dynamicConsumers} is {@code false} it is also combined with the
   *                                  worker index to form each consumer's {@code group.instance.id}
   * @param concurrencyLimit          number of parallel consumer worker threads spawned at {@link #startUp}
   * @param startupGracePeriodSeconds maximum seconds to wait for at least one broker to become reachable
   * @param groupProtocol             Kafka group protocol for the backbone's consumer workers
   * @param dynamicConsumers          {@code true} to join the backbone's consumer group as dynamic members, which
   *                                  leave the group immediately on shutdown; {@code false} to join as static
   *                                  members identified by {@code <nodeName>-<index>}
   * @param topicName                 logical topic name; the actual Kafka topic is {@code oumuamua-<topicName>}
   * @param perApplicationScope       the scope to bind on every consumer worker thread, or {@code null} to adopt
   *                                  the scope bound on the thread that constructs this backbone; with none bound
   *                                  either, the workers it starts run with no scope, carrying only the
   *                                  {@code PerApplicationContext} they inherit from the thread calling
   *                                  {@link #startUp}
   * @param servers                   one or more Kafka bootstrap broker addresses
   * @throws KafkaConnectionException if no broker is reachable within the startup grace period
   * @throws ScopeException           if a scope was given and a different scope is bound on the constructing thread
   */
  public KafkaBackbone (String nodeName, int concurrencyLimit, int startupGracePeriodSeconds, KafkaGroupProtocol groupProtocol, boolean dynamicConsumers, String topicName, PerApplicationScope perApplicationScope, KafkaServer... servers)
    throws KafkaConnectionException {

    this.nodeName = nodeName;
    this.concurrencyLimit = concurrencyLimit;
    this.startupGracePeriodSeconds = startupGracePeriodSeconds;
    this.groupProtocol = groupProtocol;
    this.dynamicConsumers = dynamicConsumers;
    this.topicName = topicName;
    this.perApplicationScope = PerApplicationScope.resolve(perApplicationScope);

    groupId = SnowflakeId.newInstance().generateHexEncoding();

    LoggerManager.getLogger(KafkaBackbone.class).info("Starting Kafka backbone...");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);
    LoggerManager.getLogger(KafkaBackbone.class).info("Started Kafka backbone with bootstrap servers(%s)...", connector.getBoostrapServers());

    prefixedTopicName = "oumuamua-" + topicName;
    producer = connector.createProducer("oumuamua-producer-" + topicName + "-" + nodeName);
  }

  /**
   * Creates and subscribes a new Kafka consumer for the worker at {@code index}.
   * Each worker uses the same group ID so the full fan-out is preserved.  When static membership is
   * in use each worker also gets a distinct {@code group.instance.id} of {@code <nodeName>-<index>}
   * so the workers do not fence each other.
   *
   * @param index zero-based worker index used to form a unique consumer client ID and, for static
   *              members, the {@code group.instance.id}
   * @return a new {@link Consumer} already subscribed to the backbone topic
   */
  private Consumer<Long, byte[]> createConsumer (int index) {

    return connector.createConsumer(groupProtocol, nodeName + "-" + index, "oumuamua-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, dynamicConsumers, prefixedTopicName);
  }

  /**
   * Spawns {@code concurrencyLimit} consumer worker threads and transitions the backbone to
   * {@link ComponentStatus#STARTED}.  Blocks if a concurrent state transition is in progress.
   *
   * @param server server to which deserialized packets from remote nodes will be delivered
   * @throws ComponentStateException if the backbone has already been shut down (it is terminal once stopped) or
   *                                 cannot reach the started state
   * @throws InterruptedException    if interrupted while waiting for the state transition
   */
  @Override
  public void startUp (Server<V> server)
    throws ComponentStateException, InterruptedException {

    if (componentModulator.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      if (resourcesReleased.get()) {
        componentModulator.set(ComponentStatus.STOPPED);
        throw new ComponentStateException("The backbone has been shut down and can not be started again");
      }

      workers = new ConsumerWorker[concurrencyLimit];

      for (int index = 0; index < concurrencyLimit; index++) {

        ConsumerWorker<V> worker = workers[index] = new ConsumerWorker<V>(server, nodeName, index);
        Thread workerThread = new Thread((perApplicationScope == null) ? worker : () -> perApplicationScope.run(worker));

        workerThread.setName("oumuamua-kafka-consumer-" + nodeName + "-" + index);
        workerThread.setDaemon(false);
        workerThread.start();
      }
      componentModulator.set(ComponentStatus.STARTED);
    } else if (ComponentStatus.STOPPED.equals(componentModulator.awaitIn(ComponentStatus.STOPPED, ComponentStatus.STARTED))) {
      throw new ComponentStateException("Could not enter the started state");
    }
  }

  /**
   * Signals every consumer worker to stop and wakes it out of its poll, waits for each to exit, shuts down the send
   * executor and waits up to the configured startup grace period for queued send tasks to hand their records to the
   * producer, closes the
   * producer — waiting up to the same grace period for buffered records to reach the broker, after which any still
   * unsent are abandoned — and transitions the backbone to {@link ComponentStatus#STOPPED}. Once this call has taken
   * the stopping transition, the executor and the producer are released even when a step throws or the thread is
   * interrupted, and the backbone reaches {@code STOPPED} regardless. Calling this on a backbone that was never
   * started releases the producer and the executor too. The backbone is terminal once stopped: a later
   * {@link #publish} is dropped and logged rather than sent, and {@link #startUp(Server)} throws
   * {@code ComponentStateException}. Blocks while a concurrent state transition is in progress; the two exceptions
   * below are the only ways out of that wait that release nothing.
   *
   * @throws ComponentStateException if a concurrent {@link #startUp(Server)} completed while this call waited, so
   *                                 the backbone is {@code STARTED} and nothing was released
   * @throws InterruptedException    if interrupted while waiting for a concurrent transition to end (nothing has been
   *                                 released), or while waiting for worker exit or the executor drain (the resources
   *                                 are released before the exception propagates, and every worker has already been
   *                                 signalled, so the workers still exit on their own)
   */
  @Override
  public void shutDown ()
    throws ComponentStateException, InterruptedException {

    if (componentModulator.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      try {
        try {
          stopWorkers();
        } finally {
          releaseResources();
        }
      } finally {
        componentModulator.set(ComponentStatus.STOPPED);
      }
    } else if (componentModulator.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STOPPING)) {
      // never started, or already stopped: the transition keeps a concurrent startUp from slipping in between the
      // release and the terminal check it makes after winning its own transition
      try {
        releaseResources();
      } finally {
        componentModulator.set(ComponentStatus.STOPPED);
      }
    } else if (ComponentStatus.STARTED.equals(componentModulator.awaitIn(ComponentStatus.STOPPED, ComponentStatus.STARTED))) {
      throw new ComponentStateException("Could not enter the stopped state");
    } else {
      releaseResources();
    }
  }

  // Signals every worker before waiting on any, so an interrupt during one wait strands none of them.
  private void stopWorkers ()
    throws InterruptedException {

    for (ConsumerWorker<V> worker : workers) {
      worker.signalStop();
    }
    for (ConsumerWorker<V> worker : workers) {
      worker.awaitExit();
    }
  }

  private void releaseResources ()
    throws InterruptedException {

    if (resourcesReleased.compareAndSet(false, true)) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(startupGracePeriodSeconds, TimeUnit.SECONDS)) {
          LoggerManager.getLogger(KafkaBackbone.class).warn("Kafka backbone(%s) send executor did not drain within %d seconds; queued publishes were abandoned", prefixedTopicName, startupGracePeriodSeconds);
        }
      } finally {
        producer.close(Duration.ofSeconds(startupGracePeriodSeconds));
      }
    }
  }

  /**
   * Serializes {@code packet} and publishes it to the backbone topic via the virtual-thread executor. Serialization
   * failures, failures thrown by the producer, and failures the producer reports asynchronously through its send
   * callback are logged but not propagated; this is a best-effort fan-out. A packet published after
   * {@link #shutDown()} is dropped with a WARN.
   *
   * @param packet packet to distribute to all cluster nodes
   */
  @Override
  public void publish (Packet<V> packet) {

    if (executorService.isShutdown()) {
      LoggerManager.getLogger(KafkaBackbone.class).warn("Kafka backbone(%s) has been shut down; the packet was not published", prefixedTopicName);
    } else {
      try {
        executorService.submit(() -> {
          try {
            producer.send(new ProducerRecord<>(prefixedTopicName, RecordUtility.serialize(nodeName, packet)), (metadata, exception) -> {
              if (exception != null) {
                LoggerManager.getLogger(KafkaBackbone.class).error(exception);
              }
            });
          } catch (IOException | RuntimeException exception) {
            LoggerManager.getLogger(KafkaBackbone.class).error(exception);
          }
        });
      } catch (RejectedExecutionException rejectedExecutionException) {
        LoggerManager.getLogger(KafkaBackbone.class).warn("Kafka backbone(%s) was shut down while the packet was being published; the packet was not published", prefixedTopicName);
      }
    }
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
     * Signals the poll loop to stop and wakes the consumer out of any blocking poll, without waiting for the thread
     * to exit, so that every worker can be signalled before any is waited for.
     */
    private void signalStop () {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();
      }
    }

    /**
     * Blocks until the worker thread has fully exited.
     *
     * @throws InterruptedException if interrupted while waiting on the exit latch
     */
    private void awaitExit ()
      throws InterruptedException {

      exitLatch.await();
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
     * {@link #signalStop()} call exits the loop cleanly; unexpected wakeups are logged as errors.
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

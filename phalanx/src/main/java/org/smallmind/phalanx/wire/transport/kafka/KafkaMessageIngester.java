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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaGroupProtocol;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

/**
 * Manages a fixed pool of Kafka consumer threads that continuously poll a single topic and
 * deliver each record to a caller-supplied callback.
 *
 * <p>Consumption can be suspended via {@link #pause()} and resumed via {@link #play()} without
 * tearing down the worker threads.  On recoverable poll errors each worker automatically closes
 * and recreates its consumer, preserving its paused or playing state.  Offsets are committed
 * synchronously per partition after each batch; callback errors are logged but do not prevent
 * the commit.
 */
public class KafkaMessageIngester {

  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final ReentrantLock consumerLock = new ReentrantLock();
  private final KafkaConnector connector;
  private final java.util.function.Consumer<ConsumerRecord<Long, byte[]>> callback;
  private final KafkaGroupProtocol groupProtocol;
  private final String nodeName;
  private final String groupId;
  private final String topicName;
  private final int concurrencyLimit;
  private ConsumerWorker[] workers;

  /**
   * Creates an ingester configured for the named topic without starting any threads.
   * Call {@link #startUp()} before invoking {@link #play()} or {@link #pause()}.
   *
   * @param nodeName         label appended to consumer client IDs for broker-side diagnostics
   * @param groupId          Kafka consumer group identifier; governs offset coordination among workers
   * @param topicName        topic to subscribe to when the ingester is in the playing state
   * @param connector        factory used to create {@link org.apache.kafka.clients.consumer.Consumer} instances
   * @param groupProtocol    Kafka group protocol applied to each worker consumer
   * @param callback         invoked for every record polled from the topic
   * @param concurrencyLimit number of parallel consumer worker threads to maintain
   */
  public KafkaMessageIngester (String nodeName, String groupId, String topicName, KafkaConnector connector, KafkaGroupProtocol groupProtocol, java.util.function.Consumer<ConsumerRecord<Long, byte[]>> callback, int concurrencyLimit) {

    this.nodeName = nodeName;
    this.groupId = groupId;
    this.topicName = topicName;
    this.connector = connector;
    this.groupProtocol = groupProtocol;
    this.callback = callback;
    this.concurrencyLimit = concurrencyLimit;
  }

  /**
   * Creates a Kafka consumer for the given worker slot, optionally subscribing it to the
   * configured topic.
   *
   * @param index  zero-based worker index; incorporated into the consumer's client ID for uniqueness
   * @param paused when {@code true} the consumer is created without an initial topic subscription;
   *               when {@code false} it subscribes to the configured topic immediately
   * @return a newly created {@link Consumer}
   */
  private Consumer<Long, byte[]> createConsumer (int index, boolean paused) {

    return connector.createConsumer(groupProtocol, nodeName, "wire-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, paused ? null : topicName);
  }

  /**
   * Starts {@code concurrencyLimit} consumer worker threads and transitions the ingester to the
   * {@link ComponentStatus#STARTED} state.  If another thread is concurrently starting the
   * ingester, this call blocks until that start completes.  Returns {@code this} for chaining.
   *
   * @return this ingester instance
   * @throws InterruptedException if interrupted while waiting for a concurrent start to finish
   */
  public synchronized KafkaMessageIngester startUp ()
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      workers = new ConsumerWorker[concurrencyLimit];

      for (int index = 0; index < concurrencyLimit; index++) {
        new Thread(workers[index] = new ConsumerWorker(index)).start();
      }
      statusRef.set(ComponentStatus.STARTED);
    } else {
      while (ComponentStatus.STARTING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }

    return this;
  }

  /**
   * Subscribes all worker consumers to the configured topic, causing them to resume delivering
   * records to the callback.  Does nothing if the ingester is not in the
   * {@link ComponentStatus#STARTED} state.
   */
  public synchronized void play () {

    if (ComponentStatus.STARTED.equals(statusRef.get())) {
      for (ConsumerWorker worker : workers) {
        worker.play();
      }
    }
  }

  /**
   * Unsubscribes all worker consumers from the topic, halting record delivery without stopping
   * the underlying worker threads.  Does nothing if the ingester is not in the
   * {@link ComponentStatus#STARTED} state.
   */
  public synchronized void pause () {

    if (ComponentStatus.STARTED.equals(statusRef.get())) {
      for (ConsumerWorker worker : workers) {
        worker.pause();
      }
    }
  }

  /**
   * Signals all workers to exit their poll loops and blocks until each worker thread has stopped.
   * If another thread is concurrently shutting down the ingester, this call blocks until that
   * shutdown completes.
   *
   * @throws InterruptedException if interrupted while waiting for workers to finish
   */
  public synchronized void shutDown ()
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      for (ConsumerWorker worker : workers) {
        worker.stop();
      }
      statusRef.set(ComponentStatus.STOPPED);
    } else {
      while (ComponentStatus.STOPPING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  /**
   * Blocks until every worker consumer has been assigned its partitions and resolved a fetch
   * position for each, or the timeout elapses.  A freshly subscribed consumer receives no records
   * until the group rebalances and assigns it partitions, and because the topics use
   * {@code auto.offset.reset=latest} a partition only sits at the live log end once its position is
   * resolved; a producer that publishes before that point loses its message.  Each worker signals
   * this readiness from its own poll loop, so callers that must not drop the first message (or that
   * simply want to know the ingester is live) await it here rather than retrying throwaway sends.
   * Returns {@code false} immediately if the ingester has not been started or the timeout is not
   * positive.
   *
   * @param timeout maximum time to wait for all workers to be assigned and positioned
   * @param unit    time unit of {@code timeout}
   * @return {@code true} if every worker became ready within the timeout; {@code false} otherwise
   * @throws InterruptedException if interrupted while waiting
   */
  public boolean awaitConsumerAssignment (long timeout, TimeUnit unit)
    throws InterruptedException {

    if ((timeout <= 0) || (workers == null)) {

      return false;
    } else {

      long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);

      for (ConsumerWorker worker : workers) {

        long remainingNanos = deadlineNanos - System.nanoTime();

        if ((remainingNanos <= 0) || (!worker.assignedLatch.await(remainingNanos, TimeUnit.NANOSECONDS))) {

          return false;
        }
      }

      return true;
    }
  }

  /**
   * Single-threaded Kafka consumer that polls in a loop, dispatches each record to the ingester
   * callback, and commits offsets synchronously per partition after each batch.  Supports
   * dynamic subscribe and unsubscribe via {@link #play()} and {@link #pause()}.  On any
   * recoverable poll error the underlying consumer is closed and recreated before the next poll,
   * preserving the current paused or playing state.
   */
  private class ConsumerWorker implements Runnable {

    private final CountDownLatch assignedLatch = new CountDownLatch(1);
    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final int index;
    private Consumer<Long, byte[]> consumer;
    private boolean paused = false;

    /**
     * Creates a worker for the given slot and immediately subscribes its consumer to the topic.
     *
     * @param index zero-based worker index used to compose a unique consumer client ID
     */
    public ConsumerWorker (int index) {

      this.index = index;

      consumer = createConsumer(index, false);
    }

    /**
     * Wakes the consumer out of its current poll call and blocks until the worker thread
     * has fully exited.
     *
     * @throws InterruptedException if interrupted while awaiting the exit latch
     */
    private void stop ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();

        exitLatch.await();
      }
    }

    /**
     * Subscribes this worker's consumer to the configured topic so that record delivery resumes.
     */
    public synchronized void play () {

      consumerLock.lock();
      try {
        consumer.subscribe(Collections.singleton(topicName));
        paused = false;
      } finally {
        consumerLock.unlock();
      }
    }

    /**
     * Unsubscribes this worker's consumer from all topics, halting record delivery without
     * terminating the poll thread.
     */
    public synchronized void pause () {

      consumerLock.lock();
      try {
        consumer.unsubscribe();
        paused = true;
      } finally {
        consumerLock.unlock();
      }
    }

    /**
     * Trips this worker's {@link #assignedLatch} once its consumer has been assigned partitions and a
     * fetch position has been resolved for each.  Called from the poll loop on the worker thread (the
     * consumer's owning thread) while the consumer lock is held, so the single-threaded consumer is
     * accessed safely; under {@code auto.offset.reset=latest} resolving the position is what places
     * each partition at the live log end, after which a producer's next send is delivered rather than
     * skipped.  A no-op once the latch has already been tripped.
     */
    private void confirmAssignment () {

      Set<TopicPartition> assignment;

      if ((assignedLatch.getCount() > 0) && (!(assignment = consumer.assignment()).isEmpty())) {
        for (TopicPartition partition : assignment) {
          consumer.position(partition);
        }

        assignedLatch.countDown();
      }
    }

    /**
     * Closes the current consumer and replaces it with a freshly created one after a recoverable
     * poll error.  The replacement preserves the worker's current paused or playing state: it
     * subscribes to the configured topic only when the worker is playing, and starts without a
     * subscription when the worker is paused.  The new consumer is installed even if closing the
     * old one fails.
     */
    private synchronized void recreateConsumer () {

      try {
        consumer.unsubscribe();
        consumer.close();
      } finally {
        consumer = createConsumer(index, paused);
      }
    }

    /**
     * Main poll loop.  Polls Kafka with a 3-second timeout per iteration, invokes the callback
     * for every record, commits offsets synchronously after each per-partition batch, recreates
     * the consumer on recoverable errors, and releases the exit latch upon termination.
     */
    @Override
    public void run () {

      try {
        while (!finished.get()) {
          try {

            ConsumerRecords<Long, byte[]> records;

            if (consumerLock.tryLock(1, TimeUnit.SECONDS)) {
              try {
                if (paused) {
                  // We are paused, so we need to wait for the next call to play() and we're avoiding some very fast cycle doing nothing
                  Thread.sleep(1000);
                } else {

                  records = consumer.poll(Duration.ofSeconds(3));

                  confirmAssignment();

                  if ((records != null) && (!records.isEmpty())) {
                    for (TopicPartition partition : records.partitions()) {

                      List<ConsumerRecord<Long, byte[]>> recordList;
                      long lastOffset = 0;

                      for (ConsumerRecord<Long, byte[]> record : recordList = records.records(partition)) {
                        try {
                          callback.accept(record);
                        } catch (Exception exception) {
                          LoggerManager.getLogger(KafkaMessageIngester.class).error(exception);
                        }

                        lastOffset = record.offset();
                      }

                      if (!recordList.isEmpty()) {
                        consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                      }
                    }
                  }
                }
              } finally {
                consumerLock.unlock();
              }
            }
          } catch (InterruptedException interruptedException) {
            LoggerManager.getLogger(KafkaMessageIngester.class).error(interruptedException);
          } catch (WakeupException wakeupException) {
            if (!finished.get()) {
              LoggerManager.getLogger(KafkaMessageIngester.class).error(wakeupException);
              recreateConsumer();
            }
          } catch (Exception exception) {
            LoggerManager.getLogger(KafkaMessageIngester.class).error(exception);
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

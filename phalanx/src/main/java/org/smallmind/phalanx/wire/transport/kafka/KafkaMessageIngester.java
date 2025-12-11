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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.scribe.pen.LoggerManager;

public class KafkaMessageIngester {

  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final KafkaConnector connector;
  private final java.util.function.Consumer<ConsumerRecord<Long, byte[]>> callback;
  private final String nodeName;
  private final String groupId;
  private final String topicName;
  private final int concurrencyLimit;
  private ConsumerWorker[] workers;

  public KafkaMessageIngester (String nodeName, String groupId, String topicName, KafkaConnector connector, java.util.function.Consumer<ConsumerRecord<Long, byte[]>> callback, int concurrencyLimit) {

    this.nodeName = nodeName;
    this.groupId = groupId;
    this.topicName = topicName;
    this.connector = connector;
    this.callback = callback;
    this.concurrencyLimit = concurrencyLimit;
  }

  private Consumer<Long, byte[]> createConsumer (int index, boolean paused) {

    return connector.createConsumer("wire-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, paused ? null : topicName);
  }

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

  public synchronized void play () {

    if (ComponentStatus.STARTED.equals(statusRef.get())) {
      for (ConsumerWorker worker : workers) {
        worker.play();
      }
    }
  }

  public synchronized void pause () {

    if (ComponentStatus.STARTED.equals(statusRef.get())) {
      for (ConsumerWorker worker : workers) {
        worker.pause();
      }
    }
  }

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

  private class ConsumerWorker implements Runnable {

    private final CountDownLatch exitLatch = new CountDownLatch(1);
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final int index;
    private Consumer<Long, byte[]> consumer;
    private boolean paused = false;

    public ConsumerWorker (int index) {

      this.index = index;

      consumer = createConsumer(index, false);
    }

    private void stop ()
      throws InterruptedException {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();

        exitLatch.await();
      }
    }

    public synchronized void play () {

      consumer.subscribe(Collections.singleton(topicName));
      paused = false;
    }

    public synchronized void pause () {

      consumer.unsubscribe();
      paused = true;
    }

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
          } catch (Exception exception) {
            LoggerManager.getLogger(KafkaMessageIngester.class).error(exception);

            synchronized (this) {
              try {
                consumer.unsubscribe();
                consumer.close();
              } finally {
                consumer = createConsumer(index, paused);
              }
            }
          }
        }
      } catch (WakeupException wakeupException) {
        if (!finished.get()) {
          LoggerManager.getLogger(KafkaMessageIngester.class).error(wakeupException);
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

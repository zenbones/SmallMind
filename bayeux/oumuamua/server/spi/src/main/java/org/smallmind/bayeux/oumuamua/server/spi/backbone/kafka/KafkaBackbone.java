/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020 David Berkman
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.smallmind.bayeux.oumuamua.server.api.OumuamuaException;
import org.smallmind.bayeux.oumuamua.server.api.Packet;
import org.smallmind.bayeux.oumuamua.server.api.Server;
import org.smallmind.bayeux.oumuamua.server.api.backbone.Backbone;
import org.smallmind.bayeux.oumuamua.server.api.json.Value;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.DebonedPacket;
import org.smallmind.bayeux.oumuamua.server.spi.backbone.RecordUtility;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class KafkaBackbone<V extends Value<V>> implements Backbone<V> {

  private final ExecutorService executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final KafkaConnector connector;
  private final Producer<Long, byte[]> producer;
  private final String nodeName;
  private final String topicName;
  private final String prefixedTopicName;
  private final String groupId;
  private final int concurrencyLimit;
  private ConsumerWorker<V>[] workers;

  public KafkaBackbone (String nodeName, int concurrencyLimit, int startupGracePeriodSeconds, String topicName, KafkaServer... servers)
    throws OumuamuaException {

    long startTimestamp = System.currentTimeMillis();

    this.nodeName = nodeName;
    this.concurrencyLimit = concurrencyLimit;
    this.topicName = topicName;

    groupId = SnowflakeId.newInstance().generateHexEncoding();
    connector = new KafkaConnector(servers);
    prefixedTopicName = "oumuamua-" + topicName;
    producer = connector.createProducer("oumuamua-producer-" + topicName + "-" + nodeName);

    if (!connector.invokeAdminClient(adminClient -> {
        while (true) {
          try {
            Collection<Node> nodes = adminClient.describeCluster().nodes().get();

            return (nodes != null) && (!nodes.isEmpty());
          } catch (ExecutionException | InterruptedException exception) {
            if ((System.currentTimeMillis() - startTimestamp) < (startupGracePeriodSeconds * 1000L)) {
              try {
                Thread.sleep(1000);
              } catch (InterruptedException interruptedException) {
                LoggerManager.getLogger(KafkaBackbone.class).error(interruptedException);

                return false;
              }
            } else {
              LoggerManager.getLogger(KafkaBackbone.class).error(exception);

              return false;
            }
          }
        }
      }
    )) {
      throw new OumuamuaException("Unable to start the kafka backbone service");
    }
  }

  @Override
  public void startUp (Server<V> server)
    throws Exception {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      workers = new ConsumerWorker[concurrencyLimit];
      for (int index = 0; index < concurrencyLimit; index++) {
        new Thread(workers[index] = new ConsumerWorker<V>(server, nodeName, connector.createConsumer("oumuamua-consumer-" + index + "-" + topicName + "-" + nodeName, groupId, prefixedTopicName))).start();
      }
      statusRef.set(ComponentStatus.STARTED);
    } else {
      while (ComponentStatus.STARTING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  @Override
  public void shutDown ()
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STARTED, ComponentStatus.STOPPING)) {
      for (ConsumerWorker<V> worker : workers) {
        worker.stop();
      }
      statusRef.set(ComponentStatus.STOPPED);
    } else {
      while (ComponentStatus.STOPPING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

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

  private static class ConsumerWorker<V extends Value<V>> implements Runnable {

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final Server<V> server;
    private final Consumer<Long, byte[]> consumer;
    private final String nodeName;

    public ConsumerWorker (Server<V> server, String nodeName, Consumer<Long, byte[]> consumer) {

      this.server = server;
      this.nodeName = nodeName;
      this.consumer = consumer;
    }

    private void stop () {

      if (finished.compareAndSet(false, true)) {
        consumer.wakeup();
      }
    }

    @Override
    public void run () {

      try {
        while (!finished.get()) {

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
        }
      } catch (WakeupException wakeupException) {
        if (!finished.get()) {
          LoggerManager.getLogger(KafkaBackbone.class).error(wakeupException);
        }
      } finally {
        consumer.close();
      }
    }
  }
}

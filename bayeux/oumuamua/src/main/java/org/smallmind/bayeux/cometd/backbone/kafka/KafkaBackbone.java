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
package org.smallmind.bayeux.cometd.backbone.kafka;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
import org.smallmind.bayeux.cometd.OumuamuaException;
import org.smallmind.bayeux.cometd.OumuamuaServer;
import org.smallmind.bayeux.cometd.backbone.ClusteredTransport;
import org.smallmind.bayeux.cometd.backbone.PacketCodec;
import org.smallmind.bayeux.cometd.backbone.ServerBackbone;
import org.smallmind.bayeux.cometd.message.OumuamuaPacket;
import org.smallmind.nutsnbolts.util.ComponentStatus;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.scribe.pen.LoggerManager;

public class KafkaBackbone implements ServerBackbone {

  private static final ClusteredTransport CLUSTERED_TRANSPORT = new ClusteredTransport();
  private final AtomicReference<ComponentStatus> statusRef = new AtomicReference<>(ComponentStatus.STOPPED);
  private final KafkaConnector connector;
  private final Producer<Long, byte[]> producer;
  private final String nodeName;
  private final String topicName;
  private final String groupId;
  private final int concurrencyLimit;
  private ConsumerWorker[] workers;

  public KafkaBackbone (String nodeName, int concurrencyLimit, String topicName, KafkaServer... servers)
    throws OumuamuaException {

    this.nodeName = nodeName;
    this.concurrencyLimit = concurrencyLimit;
    this.topicName = topicName;

    groupId = SnowflakeId.newInstance().generateHexEncoding();
    connector = new KafkaConnector(servers);
    producer = connector.createProducer(nodeName);

    if (!connector.invokeAdminClient(adminClient -> {
        try {
          Collection<Node> nodes = adminClient.describeCluster().nodes().get();

          return (nodes != null) && (!nodes.isEmpty());
        } catch (ExecutionException | InterruptedException exception) {
          LoggerManager.getLogger(KafkaBackbone.class).error(exception);

          return false;
        }
      }
    )) {
      throw new OumuamuaException("Unable to start the kafka backbone service");
    }
  }

  public void startUp (OumuamuaServer oumuamuaServer)
    throws InterruptedException {

    if (statusRef.compareAndSet(ComponentStatus.STOPPED, ComponentStatus.STARTING)) {
      workers = new ConsumerWorker[concurrencyLimit];
      for (int index = 0; index < concurrencyLimit; index++) {
        new Thread(workers[index] = new ConsumerWorker(oumuamuaServer, nodeName, connector.createConsumer(nodeName + "-" + index, groupId, topicName))).start();
      }
      statusRef.set(ComponentStatus.STARTED);
    } else {
      while (ComponentStatus.STARTING.equals(statusRef.get())) {
        Thread.sleep(100);
      }
    }
  }

  public void shutDown ()
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

  @Override
  public void publish (OumuamuaPacket packet)
    throws IOException {

    producer.send(new ProducerRecord<>(topicName, PacketCodec.encode(nodeName, packet)));
  }

  private static class ConsumerWorker implements Runnable {

    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final OumuamuaServer oumuamuaServer;
    private final Consumer<Long, byte[]> consumer;
    private final String nodeName;

    public ConsumerWorker (OumuamuaServer oumuamuaServer, String nodeName, Consumer<Long, byte[]> consumer) {

      this.oumuamuaServer = oumuamuaServer;
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

                  OumuamuaPacket packet;

                  if ((packet = PacketCodec.decode(nodeName, oumuamuaServer, record.value())) != null) {
                    oumuamuaServer.publishToChannel(CLUSTERED_TRANSPORT, packet.getChannelId().getId(), packet);
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
/*
 * Copyright (c) 2007 through 2024 David Berkman
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.WorkerFactory;

public class KafkaResponseTransport extends WorkManager<InvocationWorker, ConsumerRecord<Long, byte[]>> implements WorkerFactory<InvocationWorker, ConsumerRecord<Long, byte[]>>, ResponseTransport, ResponseTransmitter {

  private final ReentrantReadWriteLock producerLock = new ReentrantReadWriteLock();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final KafkaConnector connector;
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final SignalCodec signalCodec;
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final TopicNames topicNames;
  private final ConcurrentHashMap<String, Producer<Long, byte[]>> producerMap = new ConcurrentHashMap<>();
  private final String nodeName;
  private final String serviceGroup;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  public KafkaResponseTransport (String nodeName, String serviceGroup, Class<InvocationWorker> workerClass, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, KafkaServer... servers) {

    super(workerClass, concurrencyLimit);

    this.nodeName = nodeName;
    this.serviceGroup = serviceGroup;
    this.signalCodec = signalCodec;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers);
  }

  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  @Override
  public String getInstanceId () {

    return instanceId;
  }

  @Override
  public InvocationWorker createWorker (WorkQueue<ConsumerRecord<Long, byte[]>> workQueue) {

    return new InvocationWorker(workQueue, this, invocationCircuit, signalCodec);
  }

  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  @Override
  public void play ()
    throws Exception {

  }

  @Override
  public void pause ()
    throws Exception {

  }

  private Producer<Long, byte[]> getProducer (String topic) {

    producerLock.readLock().lock();
    try {
      return closed.get() ? null : producerMap.computeIfAbsent(topic, alsoTopic -> connector.createProducer("wire-producer-" + alsoTopic + "-" + nodeName));
    } finally {
      producerLock.readLock().unlock();
    }
  }

  @Override
  public void transmit (String callerId, String correlationId, boolean error, String nativeType, Object result)
    throws Throwable {

    Producer<Long, byte[]> responseProducer;
    String topic;

    if ((responseProducer = getProducer(topic = topicNames.getResponseTopicName(serviceGroup, callerId))) == null) {
      throw new AlreadyClosedException();
    } else {

      ProducerRecord<Long, byte[]> record = new ProducerRecord<>(topic, signalCodec.encode(new ResultSignal(error, nativeType, result)));
      String messageId = SnowflakeId.newInstance().generateDottedString();

      record.headers().add("messageId", messageId.getBytes());
      record.headers().add("correlationId", correlationId.getBytes());

      responseProducer.send(record);
    }
  }

  @Override
  public void close ()
    throws Exception {

    producerLock.writeLock().lock();
    try {
      if (closed.compareAndExchange(false, true)) {
        for (Producer<Long, byte[]> producer : producerMap.values()) {
          producer.close();
        }
      }
    } finally {
      producerLock.writeLock().unlock();
    }
  }
}

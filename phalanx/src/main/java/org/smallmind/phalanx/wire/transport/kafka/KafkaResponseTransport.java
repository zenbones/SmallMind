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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.kafka.utility.KafkaConnectionException;
import org.smallmind.kafka.utility.KafkaConnector;
import org.smallmind.kafka.utility.KafkaServer;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.ResponseTransport;
import org.smallmind.phalanx.wire.transport.TransportState;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WiredService;
import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RequestMessageRouter;
import org.smallmind.phalanx.worker.WorkManager;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.WorkerFactory;

public class KafkaResponseTransport extends WorkManager<InvocationWorker, ConsumerRecord<Long, byte[]>> implements WorkerFactory<InvocationWorker, ConsumerRecord<Long, byte[]>>, ResponseTransport, ResponseTransmitter {

  private final ExecutorService executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
  private final ReentrantReadWriteLock producerLock = new ReentrantReadWriteLock();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final KafkaConnector connector;
  private final AtomicReference<TransportState> transportStateRef = new AtomicReference<>(TransportState.PLAYING);
  private final SignalCodec signalCodec;
  private final WireInvocationCircuit invocationCircuit = new WireInvocationCircuit();
  private final TopicNames topicNames;
  private final KafkaMessageIngester whisperMessageIngester;
  private final KafkaMessageIngester talkMessageIngester;
  private final KafkaMessageIngester shoutMessageIngester;
  private final ConcurrentHashMap<String, Producer<Long, byte[]>> producerMap = new ConcurrentHashMap<>();
  private final String nodeName;
  private final String instanceId = SnowflakeId.newInstance().generateDottedString();

  public KafkaResponseTransport (String nodeName, String serviceGroup, Class<InvocationWorker> workerClass, SignalCodec signalCodec, int concurrencyLimit, int startupGracePeriodSeconds, KafkaServer... servers)
    throws KafkaConnectionException, InterruptedException {

    super(workerClass, concurrencyLimit);

    ResponseCallback responseCallback = new ResponseCallback(this);

    this.nodeName = nodeName;
    this.signalCodec = signalCodec;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);

    whisperMessageIngester = new KafkaMessageIngester(nodeName, instanceId, topicNames.getWhisperTopicName(serviceGroup, instanceId), connector, responseCallback, concurrencyLimit).startUp();
    talkMessageIngester = new KafkaMessageIngester(nodeName, "wire-talk", topicNames.getTalkTopicName(serviceGroup), connector, responseCallback, concurrencyLimit).startUp();
    shoutMessageIngester = new KafkaMessageIngester(nodeName, instanceId, topicNames.getShoutTopicName(serviceGroup), connector, responseCallback, concurrencyLimit).startUp();
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
  public synchronized void play ()
    throws Exception {

    if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
      whisperMessageIngester.play();
      talkMessageIngester.play();
      shoutMessageIngester.play();
    }
  }

  @Override
  public synchronized void pause ()
    throws Exception {

    if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
      whisperMessageIngester.pause();
      talkMessageIngester.pause();
      shoutMessageIngester.pause();
    }
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

    if ((responseProducer = getProducer(topic = topicNames.getResponseTopicName(callerId))) == null) {
      throw new AlreadyClosedException();
    } else {

      ProducerRecord<Long, byte[]> record = Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(
        () -> new ProducerRecord<>(topic, signalCodec.encode(new ResultSignal(error, nativeType, result)))
      );
      String messageId = SnowflakeId.newInstance().generateDottedString();

      record.headers().add(HeaderUtility.MESSAGE_ID, messageId.getBytes());
      record.headers().add(HeaderUtility.CORRELATION_ID, correlationId.getBytes());

      executorService.submit(() -> responseProducer.send(record));
    }
  }

  @Override
  public synchronized void close ()
    throws Exception {

    transportStateRef.set(TransportState.CLOSED);

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

    whisperMessageIngester.shutDown();
    talkMessageIngester.shutDown();
    shoutMessageIngester.shutDown();
  }
}

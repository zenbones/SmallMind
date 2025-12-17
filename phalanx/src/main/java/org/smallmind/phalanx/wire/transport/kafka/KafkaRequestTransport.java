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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import org.smallmind.phalanx.wire.ConversationType;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.Whispering;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.AbstractRequestTransport;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.amqp.rabbitmq.RequestMessageRouter;

/**
 * Kafka-backed implementation of {@link org.smallmind.phalanx.wire.transport.RequestTransport} that publishes invocation
 * messages and waits for correlated responses on a dedicated response topic.
 */
public class KafkaRequestTransport extends AbstractRequestTransport {

  private final ExecutorService executorService = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new ThreadPoolExecutor.CallerRunsPolicy());
  private final ReentrantReadWriteLock producerLock = new ReentrantReadWriteLock();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final KafkaConnector connector;
  private final SignalCodec signalCodec;
  private final TopicNames topicNames;
  private final KafkaMessageIngester responseMessageIngester;
  private final ConcurrentHashMap<String, Producer<Long, byte[]>> producerMap = new ConcurrentHashMap<>();
  private final String nodeName;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  /**
   * Builds a transport that sends requests to service group topics and awaits responses.
   *
   * @param nodeName                  identifier appended to client names for diagnostics.
   * @param signalCodec               codec used to serialize/deserialize signals.
   * @param concurrencyLimit          number of concurrent response consumers.
   * @param defaultTimeoutSeconds     default timeout while awaiting responses.
   * @param startupGracePeriodSeconds grace period while waiting for brokers to become available.
   * @param servers                   Kafka bootstrap servers.
   * @throws KafkaConnectionException if the connector cannot reach Kafka.
   * @throws InterruptedException     if interrupted while waiting for connector startup.
   */
  public KafkaRequestTransport (String nodeName, SignalCodec signalCodec, int concurrencyLimit, long defaultTimeoutSeconds, int startupGracePeriodSeconds, KafkaServer... servers)
    throws KafkaConnectionException, InterruptedException {

    super(defaultTimeoutSeconds);

    this.signalCodec = signalCodec;
    this.nodeName = nodeName;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);

    responseMessageIngester = new KafkaMessageIngester(nodeName, callerId, topicNames.getResponseTopicName(callerId), connector, new RequestCallback(this, signalCodec), concurrencyLimit).startUp();
  }

  /**
   * Unique identifier used by downstream responders to target the caller-specific response topic.
   *
   * @return the transport caller id.
   */
  @Override
  public String getCallerId () {

    return callerId;
  }

  /**
   * Retrieves or creates a producer for the given topic while respecting the closed state.
   *
   * @param topic topic name for which to acquire a producer.
   * @return a producer ready to publish to the topic, or {@code null} if already closed.
   */
  private Producer<Long, byte[]> getProducer (String topic) {

    producerLock.readLock().lock();
    try {
      return closed.get() ? null : producerMap.computeIfAbsent(topic, alsoTopic -> connector.createProducer("wire-producer-" + alsoTopic + "-" + nodeName));
    } finally {
      producerLock.readLock().unlock();
    }
  }

  /**
   * Serializes and publishes an invocation signal to the appropriate topic, waiting for a response unless IN_ONLY.
   *
   * @param voice     invocation metadata describing conversation type and routing.
   * @param route     route to the target service/method/version.
   * @param arguments invocation arguments to encode.
   * @param contexts  optional wire contexts to include.
   * @return the decoded result for two-way conversations, or {@code null} for IN_ONLY calls.
   * @throws Throwable if message publication fails or awaiting a response raises an error.
   */
  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    Producer<Long, byte[]> requestProducer;
    String messageId = SnowflakeId.newInstance().generateDottedString();
    boolean inOnly = voice.getConversation().getConversationType().equals(ConversationType.IN_ONLY);

    String topic = switch (voice.getMode()) {
      case SHOUT -> topicNames.getShoutTopicName((String)voice.getServiceGroup());
      case TALK -> topicNames.getTalkTopicName((String)voice.getServiceGroup());
      case WHISPER -> topicNames.getWhisperTopicName((String)voice.getServiceGroup(), ((Whispering)voice).getInstanceId());
    };

    if ((requestProducer = getProducer(topic)) == null) {
      throw new AlreadyClosedException();
    } else {

      ProducerRecord<Long, byte[]> record = Instrument.with(RequestMessageRouter.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.CONSTRUCT_MESSAGE.getDisplay())).on(
        () -> new ProducerRecord<>(topic, signalCodec.encode(new InvocationSignal(inOnly, route, arguments, contexts)))
      );

      record.headers().add(HeaderUtility.MESSAGE_ID, messageId.getBytes());
      record.headers().add(HeaderUtility.CALLER_ID, callerId.getBytes());

      executorService.submit(() -> requestProducer.send(record));

      return Instrument.with(KafkaRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_RESULT.getDisplay())).on(
        () -> acquireResult(signalCodec, route, voice, messageId, inOnly)
      );
    }
  }

  /**
   * Closes producers and shuts down the response ingester.
   *
   * @throws Exception if closing resources fails.
   */
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

    responseMessageIngester.shutDown();
  }
}

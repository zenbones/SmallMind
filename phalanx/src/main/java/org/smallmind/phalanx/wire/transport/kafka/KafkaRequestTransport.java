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
 * Kafka implementation of {@link org.smallmind.phalanx.wire.transport.RequestTransport} that publishes invocation requests and
 * listens for responses.
 * <p>
 * Requests are encoded with a {@link SignalCodec} and routed to whisper, talk or shout topics depending on the selected
 * {@link Voice} mode. Responses are ingested on the caller-specific response topic and forwarded back to waiting callers.
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
   * Creates a request transport capable of sending invocation signals and listening for responses.
   *
   * @param nodeName                  logical node identifier used for producer naming
   * @param signalCodec               codec used to encode invocations and decode responses
   * @param concurrencyLimit          number of consumer workers to ingest responses
   * @param defaultTimeoutSeconds     default timeout for awaiting responses
   * @param startupGracePeriodSeconds grace period for establishing Kafka connectivity
   * @param servers                   Kafka server definitions used to create connections
   * @throws KafkaConnectionException if Kafka connectivity cannot be established
   * @throws InterruptedException     if startup is interrupted while waiting for consumers
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

  @Override
  /**
   * @return the unique caller identifier used to build the response topic name
   */
  public String getCallerId () {

    return callerId;
  }

  /**
   * Retrieves or creates a producer for the given topic unless the transport has been closed.
   *
   * @param topic topic for which a producer is required
   * @return a producer if the transport is open, otherwise {@code null}
   */
  private Producer<Long, byte[]> getProducer (String topic) {

    producerLock.readLock().lock();
    try {
      return closed.get() ? null : producerMap.computeIfAbsent(topic, alsoTopic -> connector.createProducer("wire-producer-" + alsoTopic + "-" + nodeName));
    } finally {
      producerLock.readLock().unlock();
    }
  }

  @Override
  /**
   * Publishes an invocation request to the appropriate topic and optionally waits for a response.
   *
   * @param voice     descriptor that determines service group, delivery mode and conversation type
   * @param route     invocation route pointing to the target service and method
   * @param arguments invocation arguments to encode and send
   * @param contexts  optional contextual objects propagated with the invocation
   * @return the result of the invocation for request/reply conversations, or {@code null} for in-only requests
   * @throws Throwable if the transport is closed, message encoding fails, or waiting for the result produces an error
   */
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

  @Override
  /**
   * Closes the transport, preventing additional requests from being sent and shutting down the response ingester.
   *
   * @throws Exception if shutting down the ingester or producers fails
   */
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

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
import org.smallmind.kafka.utility.KafkaGroupProtocol;
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
 * Kafka-backed request transport.  Serializes {@link InvocationSignal}s onto the appropriate
 * per-service-group topic (shout, talk, or whisper) and awaits correlated
 * {@link org.smallmind.phalanx.wire.signal.ResultSignal} responses on a caller-specific
 * response topic.
 *
 * <p>A lazily-populated producer map holds one {@link Producer} per request topic.  Producers
 * are created on first use and all are closed together during {@link #close()}.  A dedicated
 * {@link KafkaMessageIngester} runs the response consumer threads and completes pending requests
 * via the parent-class callback mechanism.
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
   * Constructs the transport, verifies Kafka broker availability, and starts the response ingester.
   *
   * @param nodeName                  label appended to producer and consumer client IDs for tracing
   * @param signalCodec               codec used to serialize {@link InvocationSignal}s and deserialize results
   * @param concurrencyLimit          number of parallel response consumer threads
   * @param defaultTimeoutSeconds     seconds a caller waits for a response when no explicit timeout is provided
   * @param startupGracePeriodSeconds seconds to retry broker connectivity before throwing
   * @param groupProtocol             Kafka group protocol for the response consumer threads
   * @param servers                   Kafka bootstrap servers to connect to
   * @throws KafkaConnectionException if no broker becomes reachable within the grace period
   * @throws InterruptedException     if interrupted while the response ingester is starting
   */
  public KafkaRequestTransport (String nodeName, SignalCodec signalCodec, int concurrencyLimit, long defaultTimeoutSeconds, int startupGracePeriodSeconds, KafkaGroupProtocol groupProtocol, KafkaServer... servers)
    throws KafkaConnectionException, InterruptedException {

    super(defaultTimeoutSeconds);

    this.signalCodec = signalCodec;
    this.nodeName = nodeName;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);

    responseMessageIngester = new KafkaMessageIngester(nodeName, callerId, topicNames.getResponseTopicName(callerId), connector, groupProtocol, new RequestCallback(this, signalCodec), concurrencyLimit).startUp();
  }

  /**
   * Returns the unique caller identifier assigned to this transport instance.
   * The response transport uses this value to derive the response topic name when publishing
   * results back to this client.
   *
   * @return caller ID string
   */
  @Override
  public String getCallerId () {

    return callerId;
  }

  /**
   * Returns an existing producer for {@code topic}, creating one if none exists yet, or returns
   * {@code null} if the transport has already been closed.  A read lock guards concurrent
   * publishes against the write-locked {@link #close()} path.
   *
   * @param topic Kafka topic for which a producer is required
   * @return the producer for the given topic, or {@code null} if the transport is closed
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
   * Encodes the invocation as an {@link InvocationSignal}, selects the request topic based on
   * the voice mode (shout, talk, or whisper), publishes the record asynchronously, and — for
   * two-way conversations — blocks until the correlated response arrives or the timeout expires.
   *
   * @param voice     describes the conversation type and target service group or instance
   * @param route     identifies the target service, method name, and version
   * @param arguments named method arguments to encode in the invocation signal
   * @param contexts  optional wire contexts forwarded with the invocation
   * @return the decoded result object for two-way calls, or {@code null} for {@link ConversationType#IN_ONLY} calls
   * @throws AlreadyClosedException if the transport has been closed before or during this call
   * @throws Throwable              if signal encoding fails, the response times out, or the
   *                                result signal encodes a service-side exception
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
   * Closes all cached producers under the write lock, then shuts down the response ingester.
   * After this method returns, subsequent calls to {@link #transmit} will throw
   * {@link AlreadyClosedException}.
   *
   * @throws Exception if closing a producer or shutting down the ingester raises an error
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

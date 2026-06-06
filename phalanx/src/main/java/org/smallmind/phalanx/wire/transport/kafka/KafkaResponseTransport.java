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

import java.util.Collections;
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
import org.smallmind.kafka.utility.KafkaGroupProtocol;
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

/**
 * Kafka-based {@link ResponseTransport} and {@link ResponseTransmitter}.  Consumes invocation
 * requests from shout, talk, and whisper topics, dispatches them to a pool of
 * {@link InvocationWorker}s for execution, and publishes {@link ResultSignal}s back to the
 * caller's response topic.
 *
 * <p>Three separate {@link KafkaMessageIngester}s subscribe to the conversation-type topics for
 * a single service group.  The whisper topic is per-instance and is automatically deleted from
 * the broker when {@link #close()} is called.  Ingestion can be suspended and resumed via
 * {@link #pause()} and {@link #play()} without stopping the worker pool.
 */
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
  private final String whisperTopicName;

  /**
   * Constructs the response transport, verifies broker availability, starts the three topic
   * ingesters (whisper, talk, and shout) for the given service group, and initializes the
   * invocation worker pool.
   *
   * @param nodeName                  label appended to producer and consumer client IDs for tracing
   * @param serviceGroup              logical service group whose topics this transport subscribes to
   * @param workerClass               {@link InvocationWorker} subclass instantiated by the work manager
   * @param signalCodec               codec for encoding {@link ResultSignal}s and decoding {@link org.smallmind.phalanx.wire.signal.InvocationSignal}s
   * @param concurrencyLimit          thread count applied to both the ingesters and the invocation worker pool
   * @param startupGracePeriodSeconds seconds to retry broker connectivity before throwing
   * @param groupProtocol             Kafka group protocol for the whisper, talk, and shout consumer threads
   * @param servers                   Kafka bootstrap servers to connect to
   * @throws KafkaConnectionException if no broker becomes reachable within the grace period
   * @throws InterruptedException     if interrupted while starting ingesters or the worker pool
   */
  public KafkaResponseTransport (String nodeName, String serviceGroup, Class<InvocationWorker> workerClass, SignalCodec signalCodec, int concurrencyLimit, int startupGracePeriodSeconds, KafkaGroupProtocol groupProtocol, KafkaServer... servers)
    throws KafkaConnectionException, InterruptedException {

    super(workerClass, concurrencyLimit);

    ResponseCallback responseCallback = new ResponseCallback(this);

    this.nodeName = nodeName;
    this.signalCodec = signalCodec;

    topicNames = new TopicNames("wire");
    connector = new KafkaConnector(servers).check(startupGracePeriodSeconds);

    whisperMessageIngester = new KafkaMessageIngester(nodeName, instanceId, whisperTopicName = topicNames.getWhisperTopicName(serviceGroup, instanceId), connector, groupProtocol, responseCallback, concurrencyLimit).startUp();
    talkMessageIngester = new KafkaMessageIngester(nodeName, "wire-talk", topicNames.getTalkTopicName(serviceGroup), connector, groupProtocol, responseCallback, concurrencyLimit).startUp();
    shoutMessageIngester = new KafkaMessageIngester(nodeName, instanceId, topicNames.getShoutTopicName(serviceGroup), connector, groupProtocol, responseCallback, concurrencyLimit).startUp();

    startUp(this);
  }

  /**
   * Registers a service implementation with the invocation circuit so its methods can be
   * resolved and invoked when invocation signals arrive.
   *
   * @param serviceInterface the interface declaring the remotely callable methods
   * @param targetService    the concrete service instance and its associated metadata
   * @return this transport's instance ID; callers must supply this value when directing
   * whisper-mode requests to this specific node
   * @throws Exception if the invocation circuit rejects the registration
   */
  @Override
  public String register (Class<?> serviceInterface, WiredService targetService)
    throws Exception {

    invocationCircuit.register(serviceInterface, targetService);

    return instanceId;
  }

  /**
   * Returns the unique instance identifier assigned to this transport at creation time.
   * Callers use this value to address whisper-mode requests directly to this node.
   *
   * @return instance ID string
   */
  @Override
  public String getInstanceId () {

    return instanceId;
  }

  /**
   * Creates a new {@link InvocationWorker} wired to the shared invocation circuit and response
   * transmitter.  Called by the parent {@link WorkManager} to populate the worker pool.
   *
   * @param workQueue queue from which the worker drains inbound {@link ConsumerRecord} items
   * @return a new {@link InvocationWorker} ready to process invocation records
   */
  @Override
  public InvocationWorker createWorker (WorkQueue<ConsumerRecord<Long, byte[]>> workQueue) {

    return new InvocationWorker(workQueue, this, invocationCircuit, signalCodec);
  }

  /**
   * Returns the current lifecycle state of this transport.
   *
   * @return one of {@link TransportState#PLAYING}, {@link TransportState#PAUSED},
   * or {@link TransportState#CLOSED}
   */
  @Override
  public TransportState getState () {

    return transportStateRef.get();
  }

  /**
   * Resumes ingestion on all three topic ingesters if the transport is currently
   * {@link TransportState#PAUSED}.  Does nothing when the transport is in any other state.
   *
   * @throws Exception if resuming an ingester raises an error
   */
  @Override
  public synchronized void play ()
    throws Exception {

    if (transportStateRef.compareAndSet(TransportState.PAUSED, TransportState.PLAYING)) {
      whisperMessageIngester.play();
      talkMessageIngester.play();
      shoutMessageIngester.play();
    }
  }

  /**
   * Suspends ingestion on all three topic ingesters without shutting down the transport or
   * its worker pool.  Does nothing when the transport is in any state other than
   * {@link TransportState#PLAYING}.
   *
   * @throws Exception if pausing an ingester raises an error
   */
  @Override
  public synchronized void pause ()
    throws Exception {

    if (transportStateRef.compareAndSet(TransportState.PLAYING, TransportState.PAUSED)) {
      whisperMessageIngester.pause();
      talkMessageIngester.pause();
      shoutMessageIngester.pause();
    }
  }

  /**
   * Returns an existing response producer for {@code topic}, creating one if none exists yet,
   * or returns {@code null} if the transport is already closed.  A read lock guards concurrent
   * transmissions against the write-locked {@link #close()} path.
   *
   * @param topic Kafka topic to which a {@link ResultSignal} will be published
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
   * Encodes a {@link ResultSignal} and publishes it asynchronously to the caller's response topic.
   *
   * @param callerId      identifier of the originating caller; used to derive the response topic name
   * @param correlationId correlation ID from the originating request; attached as a message header
   *                      so the caller can match the response to its pending invocation
   * @param error         {@code true} when the result payload represents a service-side error
   * @param nativeType    Java type name of the result payload, used by the caller for deserialization
   * @param result        the return value or error object to encode in the signal
   * @throws AlreadyClosedException if the transport has already been closed
   * @throws Throwable              if signal encoding or record publication raises an error
   */
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

  /**
   * Marks the transport as closed, shuts down all three topic ingesters, closes all cached
   * response producers under the write lock, and deletes the per-instance whisper topic from
   * the broker.
   *
   * @throws Exception if shutting down an ingester, closing a producer, or deleting the whisper
   *                   topic raises an error
   */
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

    connector.invokeAdminClient(client -> client.deleteTopics(Collections.singletonList(whisperTopicName)));
  }
}

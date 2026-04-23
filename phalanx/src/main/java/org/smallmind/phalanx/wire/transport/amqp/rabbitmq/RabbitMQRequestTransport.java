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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.nutsnbolts.util.SnowflakeId;
import org.smallmind.phalanx.wire.ConversationType;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.Voice;
import org.smallmind.phalanx.wire.signal.Route;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.signal.WireContext;
import org.smallmind.phalanx.wire.transport.AbstractRequestTransport;
import org.smallmind.phalanx.wire.transport.ClaxonTag;

/**
 * RabbitMQ-backed {@link org.smallmind.phalanx.wire.transport.RequestTransport} that publishes invocation
 * signals to an exchange and correlates responses from an ephemeral reply queue.
 *
 * <p>A fixed-size pool of {@link RequestMessageRouter} instances handles concurrent publishing.
 * Callers block until a router is available, then block again (for IN_OUT calls) until the
 * correlated {@link org.smallmind.phalanx.wire.signal.ResultSignal} arrives or times out.
 */
public class RabbitMQRequestTransport extends AbstractRequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final SignalCodec signalCodec;
  private final LinkedBlockingQueue<RequestMessageRouter> routerQueue;
  private final RequestMessageRouter[] requestMessageRouters;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

  /**
   * Constructs the transport, initialises {@code clusterSize} routers, and populates the pool
   * to {@code max(clusterSize, concurrencyLimit)} entries by round-robin reuse.
   *
   * @param rabbitMQConnector            source of AMQP connections and channels
   * @param ephemeralQueueContractor     declares the per-router ephemeral reply queues
   * @param nameConfiguration            naming scheme for exchanges and queues
   * @param signalCodec                  serialises and deserialises wire signals
   * @param clusterSize                  number of distinct {@link RequestMessageRouter} instances to create
   * @param concurrencyLimit             minimum pool size; entries are reused if greater than {@code clusterSize}
   * @param defaultTimeoutSeconds        seconds to wait for a response when the caller specifies no timeout
   * @param messageTTLSeconds            per-message TTL applied at publish time
   * @param autoAcknowledge              {@code true} to ack response deliveries automatically
   * @param publisherConfirmationHandler receives AMQP publisher confirms; {@code null} disables confirms
   * @throws IOException      if a channel cannot be opened or a queue cannot be declared
   * @throws TimeoutException if the AMQP broker does not respond during initialisation
   */
  public RabbitMQRequestTransport (RabbitMQConnector rabbitMQConnector, QueueContractor ephemeralQueueContractor, NameConfiguration nameConfiguration, SignalCodec signalCodec, int clusterSize, int concurrencyLimit, long defaultTimeoutSeconds, int messageTTLSeconds, boolean autoAcknowledge, PublisherConfirmationHandler publisherConfirmationHandler)
    throws IOException, TimeoutException {

    super(defaultTimeoutSeconds);

    int routerIndex = 0;

    this.signalCodec = signalCodec;

    requestMessageRouters = new RequestMessageRouter[clusterSize];
    for (int index = 0; index < requestMessageRouters.length; index++) {
      requestMessageRouters[index] = new RequestMessageRouter(rabbitMQConnector, ephemeralQueueContractor, nameConfiguration, this, signalCodec, callerId, index, messageTTLSeconds, autoAcknowledge, publisherConfirmationHandler);
      requestMessageRouters[index].initialize();
    }

    routerQueue = new LinkedBlockingQueue<>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      routerQueue.add(requestMessageRouters[routerIndex]);
      if (++routerIndex == requestMessageRouters.length) {
        routerIndex = 0;
      }
    }
  }

  /**
   * Returns the unique caller identifier embedded in every published message so that response
   * transports can route replies back to this instance.
   *
   * @return a dotted Snowflake string that is stable for the lifetime of this transport
   */
  @Override
  public String getCallerId () {

    return callerId;
  }

  /**
   * Acquires a router from the pool, publishes the invocation signal, and—for IN_OUT
   * conversations—blocks until the correlated result arrives or the timeout elapses.
   *
   * @param voice     routing and conversation metadata for this call
   * @param route     target service, version, and function descriptor
   * @param arguments named argument map to encode into the signal
   * @param contexts  optional {@link org.smallmind.phalanx.wire.signal.WireContext} values propagated with the call
   * @return the decoded return value for IN_OUT calls; {@code null} for IN_ONLY calls
   * @throws Throwable if the publish fails, the transport is closed, the result signals an error,
   *                   or the response timeout elapses
   */
  @Override
  public Object transmit (Voice<?, ?> voice, Route route, Map<String, Object> arguments, WireContext... contexts)
    throws Throwable {

    final RequestMessageRouter requestMessageRouter = acquireRequestMessageRouter();

    try {

      String messageId;
      boolean inOnly = voice.getConversation().getConversationType().equals(ConversationType.IN_ONLY);

      messageId = requestMessageRouter.publish(inOnly, (String)voice.getServiceGroup(), voice, route, arguments, contexts);

      return Instrument.with(RabbitMQRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_RESULT.getDisplay())).on(
        () -> acquireResult(signalCodec, route, voice, messageId, inOnly)
      );
    } finally {
      routerQueue.put(requestMessageRouter);
    }
  }

  /**
   * Polls the router pool in one-second intervals until a router is available or the transport
   * has been closed.
   *
   * @return an idle {@link RequestMessageRouter} ready for publishing
   * @throws Throwable if the transport is closed before a router becomes available
   */
  private RequestMessageRouter acquireRequestMessageRouter ()
    throws Throwable {

    return Instrument.with(RabbitMQRequestTransport.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.ACQUIRE_REQUEST_TRANSPORT.getDisplay())).on(() -> {

      RequestMessageRouter messageTransmitter;

      do {
        messageTransmitter = routerQueue.poll(1, TimeUnit.SECONDS);
      } while ((!closed.get()) && (messageTransmitter == null));

      if (messageTransmitter == null) {
        throw new TransportException("Message transmission has been closed");
      }

      return messageTransmitter;
    });
  }

  /**
   * Sets the closed flag and shuts down all {@link RequestMessageRouter} instances.
   * Subsequent calls to {@link #transmit} will throw once the pool drains.
   *
   * @throws Exception if any router fails to close
   */
  @Override
  public void close ()
    throws Exception {

    if (closed.compareAndSet(false, true)) {
      for (RequestMessageRouter requestMessageRouter : requestMessageRouters) {
        requestMessageRouter.close();
      }
    }
  }
}

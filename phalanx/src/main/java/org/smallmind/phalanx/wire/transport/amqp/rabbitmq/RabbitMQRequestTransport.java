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

public class RabbitMQRequestTransport extends AbstractRequestTransport {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final SignalCodec signalCodec;
  private final LinkedBlockingQueue<RequestMessageRouter> routerQueue;
  private final RequestMessageRouter[] requestMessageRouters;
  private final String callerId = SnowflakeId.newInstance().generateDottedString();

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

  @Override
  public String getCallerId () {

    return callerId;
  }

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

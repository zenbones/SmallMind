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

import java.util.Map;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.MeterFactory;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.Worker;

/**
 * Worker that decodes RabbitMQ invocation messages and dispatches them through the invocation circuit.
 */
public class InvocationWorker extends Worker<RabbitMQMessage> {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final ResponseTransmitter responseTransmitter;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  /**
   * @param workQueue           queue supplying incoming RabbitMQ messages.
   * @param responseTransmitter transport used to publish responses.
   * @param invocationCircuit   circuit that invokes target services.
   * @param signalCodec         codec for decoding invocation signals.
   */
  public InvocationWorker (WorkQueue<RabbitMQMessage> workQueue, ResponseTransmitter responseTransmitter, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec) {

    super(workQueue);

    this.responseTransmitter = responseTransmitter;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;
  }

  /**
   * Decodes the invocation payload and performs the invocation using the circuit.
   *
   * @param message RabbitMQ message containing an invocation.
   * @throws Throwable if decoding or invocation fails.
   */
  @Override
  public void engageWork (final RabbitMQMessage message)
    throws Throwable {

    InvocationSignal invocationSignal = signalCodec.decode(message.getBody(), 0, message.getBody().length, InvocationSignal.class);

    Instrument.with(InvocationWorker.class, MeterFactory.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName()), new Tag("version", Integer.toString(invocationSignal.getRoute().getVersion()))).on(
      () -> invocationCircuit.handle(responseTransmitter, signalCodec, getCallerId(message.getProperties().getHeaders()), message.getProperties().getMessageId(), invocationSignal)
    );
  }

  /**
   * Extracts the caller id from AMQP headers if present.
   *
   * @param headers message headers.
   * @return caller id string or {@code null} when absent.
   */
  private String getCallerId (Map<String, Object> headers) {

    if ((headers != null) && (headers.containsKey(CALLER_ID_AMQP_KEY))) {

      return headers.get(CALLER_ID_AMQP_KEY).toString();
    }

    return null;
  }

  /**
   * Closes any resources owned by the worker. No-op because resources are managed externally.
   */
  @Override
  public void close () {

  }
}

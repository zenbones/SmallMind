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
package org.smallmind.phalanx.wire.transport.amqp.rabbitmq;

import java.util.Map;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.signal.InvocationSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ResponseTransmitter;
import org.smallmind.phalanx.wire.transport.WireInvocationCircuit;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.Worker;

public class InvocationWorker extends Worker<RabbitMQMessage> {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final ResponseTransmitter responseTransmitter;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  public InvocationWorker (WorkQueue<RabbitMQMessage> workQueue, ResponseTransmitter responseTransmitter, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec) {

    super(workQueue);

    this.responseTransmitter = responseTransmitter;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;
  }

  @Override
  public void engageWork (final RabbitMQMessage message)
    throws Throwable {

    InvocationSignal invocationSignal = signalCodec.decode(message.getBody(), 0, message.getBody().length, InvocationSignal.class);

    Instrument.with(InvocationWorker.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("operation", "invoke"), new Tag("service", invocationSignal.getRoute().getService()), new Tag("method", invocationSignal.getRoute().getFunction().getName())).on(
      () -> invocationCircuit.handle(responseTransmitter, signalCodec, getCallerId(message.getProperties().getHeaders()), message.getProperties().getMessageId(), invocationSignal)
    );
  }

  private String getCallerId (Map<String, Object> headers) {

    if ((headers != null) && (headers.containsKey(CALLER_ID_AMQP_KEY))) {

      return headers.get(CALLER_ID_AMQP_KEY).toString();
    }

    return null;
  }

  @Override
  public void close () {

  }
}

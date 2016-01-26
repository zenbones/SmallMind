/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 David Berkman
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
package org.smallmind.phalanx.wire.amqp.rabbitmq;

import java.util.Map;
import java.util.concurrent.TransferQueue;
import javax.jms.JMSException;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.worker.Worker;

public class InvocationWorker extends Worker<RabbitMQMessage> {

  private static final String CALLER_ID_AMQP_KEY = "x-opt-" + WireProperty.CALLER_ID.getKey();

  private final ResponseTransport responseTransport;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  public InvocationWorker (MetricConfiguration metricConfiguration, TransferQueue<RabbitMQMessage> workTransferQueue, ResponseTransport responseTransport, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec) {

    super(metricConfiguration, workTransferQueue);

    this.responseTransport = responseTransport;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;
  }

  @Override
  public void engageWork (final RabbitMQMessage message)
    throws Exception {

    final InvocationSignal invocationSignal;

    invocationSignal = signalCodec.decode(message.getBody(), 0, message.getBody().length, InvocationSignal.class);
    InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("operation", "invoke"), new MetricProperty("service", invocationSignal.getAddress().getService()), new MetricProperty("method", invocationSignal.getAddress().getFunction().getName())) {

      @Override
      public void withChronometer ()
        throws JMSException {

        invocationCircuit.handle(responseTransport, signalCodec, getCallerId(message.getProperties().getHeaders()), message.getProperties().getMessageId(), invocationSignal);
      }
    });
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

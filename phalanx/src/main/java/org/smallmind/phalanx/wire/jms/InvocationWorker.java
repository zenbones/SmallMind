/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018 David Berkman
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
package org.smallmind.phalanx.wire.jms;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.config.MetricConfiguration;
import org.smallmind.phalanx.wire.InvocationSignal;
import org.smallmind.phalanx.wire.ResponseTransport;
import org.smallmind.phalanx.wire.SignalCodec;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.WireInvocationCircuit;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.phalanx.worker.WorkQueue;
import org.smallmind.phalanx.worker.Worker;

public class InvocationWorker extends Worker<Message> {

  private final ResponseTransport responseTransport;
  private final WireInvocationCircuit invocationCircuit;
  private final SignalCodec signalCodec;

  private final byte[] buffer;

  public InvocationWorker (MetricConfiguration metricConfiguration, WorkQueue<Message> workQueue, ResponseTransport responseTransport, WireInvocationCircuit invocationCircuit, SignalCodec signalCodec, int maximumMessageLength) {

    super(metricConfiguration, workQueue);

    this.responseTransport = responseTransport;
    this.invocationCircuit = invocationCircuit;
    this.signalCodec = signalCodec;

    buffer = new byte[maximumMessageLength];
  }

  @Override
  public void engageWork (final Message message)
    throws Exception {

    if (((BytesMessage)message).getBodyLength() > buffer.length) {
      throw new TransportException("Message length exceeds maximum capacity %d > %d", ((BytesMessage)message).getBodyLength(), buffer.length);
    } else {

      final InvocationSignal invocationSignal;

      ((BytesMessage)message).readBytes(buffer);
      invocationSignal = signalCodec.decode(buffer, 0, (int)((BytesMessage)message).getBodyLength(), InvocationSignal.class);
      InstrumentationManager.execute(new ChronometerInstrument(this, new MetricProperty("operation", "invoke"), new MetricProperty("service", invocationSignal.getAddress().getService()), new MetricProperty("method", invocationSignal.getAddress().getFunction().getName())) {

        @Override
        public void withChronometer ()
          throws JMSException {

          invocationCircuit.handle(responseTransport, signalCodec, message.getStringProperty(WireProperty.CALLER_ID.getKey()), message.getJMSMessageID(), invocationSignal);
        }
      });
    }
  }

  @Override
  public void close () {

  }
}

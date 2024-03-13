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
package org.smallmind.phalanx.wire.transport.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.jms.BytesMessage;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.Topic;
import org.smallmind.claxon.registry.Instrument;
import org.smallmind.claxon.registry.Tag;
import org.smallmind.claxon.registry.meter.LazyBuilder;
import org.smallmind.claxon.registry.meter.SpeedometerBuilder;
import org.smallmind.phalanx.wire.TransportException;
import org.smallmind.phalanx.wire.signal.ResultSignal;
import org.smallmind.phalanx.wire.signal.SignalCodec;
import org.smallmind.phalanx.wire.transport.ClaxonTag;
import org.smallmind.phalanx.wire.transport.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class ResponseListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final JmsRequestTransport requestTransport;
  private final ConnectionManager responseConnectionManager;
  private final Topic responseTopic;
  private final SignalCodec signalCodec;
  private final String selector;
  private final byte[] buffer;

  public ResponseListener (JmsRequestTransport requestTransport, ConnectionManager responseConnectionManager, Topic responseTopic, SignalCodec signalCodec, String callerId, int maximumMessageLength)
    throws JMSException {

    this.requestTransport = requestTransport;
    this.responseConnectionManager = responseConnectionManager;
    this.responseTopic = responseTopic;
    this.signalCodec = signalCodec;

    buffer = new byte[maximumMessageLength];
    selector = WireProperty.CALLER_ID.getKey() + "='" + callerId + "'";

    responseConnectionManager.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return responseTopic;
  }

  @Override
  public String getMessageSelector () {

    return selector;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      responseConnectionManager.stop();
      responseConnectionManager.close();
    }
  }

  @Override
  public void onMessage (final Message message) {

    try {

      long timeInTopic = System.currentTimeMillis() - message.getLongProperty(WireProperty.CLOCK.getKey());

      LoggerManager.getLogger(ResponseListener.class).debug("response message received(%s) in %d ms...", message.getJMSMessageID(), timeInTopic);
      Instrument.with(ResponseListener.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("queue", ClaxonTag.RESPONSE_TRANSIT_TIME.getDisplay())).update((timeInTopic >= 0) ? timeInTopic : 0, TimeUnit.MILLISECONDS);

      Instrument.with(ResponseListener.class, LazyBuilder.instance(SpeedometerBuilder::new), new Tag("event", ClaxonTag.COMPLETE_CALLBACK.getDisplay())).on(() -> {

        if (((BytesMessage)message).getBodyLength() > buffer.length) {
          throw new TransportException("Message length exceeds maximum capacity %d > %d", ((BytesMessage)message).getBodyLength(), buffer.length);
        }

        ((BytesMessage)message).readBytes(buffer);
        requestTransport.completeCallback(message.getJMSCorrelationID(), signalCodec.decode(buffer, 0, (int)((BytesMessage)message).getBodyLength(), ResultSignal.class));
      });
    } catch (Throwable throwable) {
      LoggerManager.getLogger(ResponseListener.class).error(throwable);
    }
  }
}

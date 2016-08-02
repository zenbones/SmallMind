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
package org.smallmind.phalanx.wire.jms;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.phalanx.wire.MetricType;
import org.smallmind.phalanx.wire.WireProperty;
import org.smallmind.scribe.pen.LoggerManager;

public class RequestListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final JmsResponseTransport jmsResponseTransport;
  private final ConnectionManager requestConnectionManager;
  private final Destination requestDestination;
  private final String selector;

  public RequestListener (JmsResponseTransport jmsResponseTransport, ConnectionManager requestConnectionManager, Destination requestDestination, String serviceGroup, String instanceId)
    throws JMSException {

    this.jmsResponseTransport = jmsResponseTransport;
    this.requestConnectionManager = requestConnectionManager;
    this.requestDestination = requestDestination;

    selector = (instanceId == null) ? WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "'" : WireProperty.SERVICE_GROUP.getKey() + "='" + serviceGroup + "' AND " + WireProperty.INSTANCE_ID.getKey() + "='" + instanceId + "'";

    requestConnectionManager.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return requestDestination;
  }

  @Override
  public String getMessageSelector () {

    return selector;
  }

  public void play ()
    throws JMSException {

    requestConnectionManager.start();
  }

  public void pause ()
    throws JMSException {

    requestConnectionManager.stop();
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnectionManager.stop();
      requestConnectionManager.close();
    }
  }

  @Override
  public void onMessage (final Message message) {

    try {

      long timeInQueue = System.currentTimeMillis() - message.getLongProperty(WireProperty.CLOCK.getKey());

      LoggerManager.getLogger(QueueOperator.class).debug("request message received(%s) in %d ms...", message.getJMSMessageID(), timeInQueue);
      InstrumentationManager.instrumentWithChronometer(jmsResponseTransport, (timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS, new MetricProperty("queue", MetricType.REQUEST_DESTINATION_TRANSIT.getDisplay()));

      jmsResponseTransport.execute(message);
    } catch (Exception exception) {
      LoggerManager.getLogger(RequestListener.class).error(exception);
    }
  }
}

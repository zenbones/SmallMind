/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012 David Berkman
 * 
 * This file is part of the SmallMind Code Project.
 * 
 * The SmallMind Code Project is free software, you can redistribute
 * it and/or modify it under the terms of GNU Affero General Public
 * License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 * 
 * The SmallMind Code Project is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the the GNU Affero General Public
 * License, along with The SmallMind Code Project. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under the GNU Affero GPL version 3 section 7
 * ------------------------------------------------------------------
 * If you modify this Program, or any covered work, by linking or
 * combining it with other code, such other code is not for that reason
 * alone subject to any of the requirements of the GNU Affero GPL
 * version 3.
 */
package org.smallmind.quorum.transport.message;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import org.smallmind.instrument.Chronometer;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.instrument.MetricRegistry;
import org.smallmind.instrument.MetricRegistryFactory;
import org.smallmind.instrument.Metrics;
import org.smallmind.quorum.transport.Transport;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.scribe.pen.LoggerManager;

public class ReceptionListener implements MessageListener {

  private static final Transport TRANSPORT;
  private static final MetricRegistry METRIC_REGISTRY;

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final QueueConnection requestConnection;
  private final QueueSession requestSession;
  private final QueueReceiver requestReceiver;
  private final SynchronousQueue<Message> messageRendezvous;

  static {

    if (((TRANSPORT = TransportManager.getTransport()) == null) || (!TRANSPORT.getMetricConfiguration().isInstrumented())) {
      METRIC_REGISTRY = null;
    }
    else {
      if ((METRIC_REGISTRY = MetricRegistryFactory.getMetricRegistry()) == null) {
        throw new ExceptionInInitializerError("No MetricRegistry instance has been registered with the MetricRegistryFactory");
      }
    }
  }

  public ReceptionListener (QueueConnection requestConnection, Queue requestQueue, AcknowledgeMode acknowledgeMode, SynchronousQueue<Message> messageRendezvous)
    throws JMSException {

    this.requestConnection = requestConnection;
    this.messageRendezvous = messageRendezvous;

    requestSession = requestConnection.createQueueSession(false, acknowledgeMode.getJmsValue());
    requestReceiver = requestSession.createReceiver(requestQueue);
    requestReceiver.setMessageListener(this);
    requestConnection.start();
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnection.stop();

      requestReceiver.close();
      requestSession.close();
      requestConnection.close();
    }
  }

  @Override
  public synchronized void onMessage (Message message) {

    boolean success;

    try {

      Chronometer rendezvousChronometer = null;
      long rendezvousStart = 0;

      if (METRIC_REGISTRY != null) {
        rendezvousChronometer = METRIC_REGISTRY.ensure(Metrics.buildChronometer(TRANSPORT.getMetricConfiguration().getChronometerSamples(), TimeUnit.MILLISECONDS, TRANSPORT.getMetricConfiguration().getTickInterval(), TRANSPORT.getMetricConfiguration().getTickTimeUnit()), TRANSPORT.getMetricConfiguration().getMetricDomain().getDomain(), new MetricProperty("event", MetricEvent.ACQUIRE_WORKER.getDisplay()));
        rendezvousStart = System.currentTimeMillis();
      }

      do {
        success = messageRendezvous.offer(message, 1, TimeUnit.SECONDS);
      } while ((!closed.get()) && (!success));

      if (METRIC_REGISTRY != null) {
        rendezvousChronometer.update(System.currentTimeMillis() - rendezvousStart, TimeUnit.MILLISECONDS);
      }
    }
    catch (Exception exception) {
      LoggerManager.getLogger(ReceptionListener.class).error(exception);
    }
  }
}

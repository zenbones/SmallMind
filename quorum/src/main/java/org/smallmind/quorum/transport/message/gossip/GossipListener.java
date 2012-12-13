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
package org.smallmind.quorum.transport.message.gossip;

import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Topic;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricDestination;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.quorum.transport.message.ConnectionFactor;
import org.smallmind.quorum.transport.message.QueueOperator;
import org.smallmind.quorum.transport.message.SessionEmployer;
import org.smallmind.scribe.pen.LoggerManager;

public class GossipListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ConnectionFactor gossipConnectionFactor;
  private final Topic gossipTopic;
  private final SynchronousQueue<Message> messageRendezvous;

  public GossipListener (ConnectionFactor gossipConnectionFactor, Topic gossipTopic, SynchronousQueue<Message> messageRendezvous)
    throws JMSException {

    this.gossipConnectionFactor = gossipConnectionFactor;
    this.gossipTopic = gossipTopic;
    this.messageRendezvous = messageRendezvous;

    this.gossipConnectionFactor.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return gossipTopic;
  }

  @Override
  public String getMessageSelector () {

    return null;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      gossipConnectionFactor.stop();
      gossipConnectionFactor.close();
    }
  }

  @Override
  public synchronized void onMessage (final Message message) {

    try {

      long timeInQueue = System.currentTimeMillis() - message.getJMSTimestamp();

      LoggerManager.getLogger(QueueOperator.class).debug("gossip message received(%s)...", message.getJMSMessageID());
      InstrumentationManager.instrumentWithChronometer(TransportManager.getTransport(), (timeInQueue >= 0) ? timeInQueue : 0, TimeUnit.MILLISECONDS, new MetricProperty("destination", MetricDestination.GOSSIP_TOPIC.getDisplay()));
      InstrumentationManager.execute(new ChronometerInstrument(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.ACQUIRE_WORKER.getDisplay())) {

        @Override
        public void withChronometer ()
          throws InterruptedException {

          boolean success;

          do {
            success = messageRendezvous.offer(message, 1, TimeUnit.SECONDS);
          } while ((!closed.get()) && (!success));
        }
      });
    }
    catch (Exception exception) {
      LoggerManager.getLogger(GossipListener.class).error(exception);
    }
  }
}

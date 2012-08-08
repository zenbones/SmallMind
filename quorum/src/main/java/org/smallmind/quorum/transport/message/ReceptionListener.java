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
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import org.smallmind.instrument.ChronometerInstrument;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricDestination;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.scribe.pen.LoggerManager;

public class ReceptionListener implements SessionEmployer, MessageListener {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ConnectionFactor requestConnectionFactor;
  private final Queue requestQueue;
  private final SynchronousQueue<Message> messageRendezvous;

  public ReceptionListener (ConnectionFactor requestConnectionFactor, Queue requestQueue, SynchronousQueue<Message> messageRendezvous)
    throws JMSException {

    this.requestConnectionFactor = requestConnectionFactor;
    this.requestQueue = requestQueue;
    this.messageRendezvous = messageRendezvous;

    requestConnectionFactor.createConsumer(this);
  }

  @Override
  public Destination getDestination () {

    return requestQueue;
  }

  @Override
  public String getMessageSelector () {

    return null;
  }

  public void close ()
    throws JMSException {

    if (closed.compareAndSet(false, true)) {
      requestConnectionFactor.stop();
      requestConnectionFactor.close();
    }
  }

  @Override
  public synchronized void onMessage (final Message message) {

    try {

      long timeInQueue = System.currentTimeMillis() - message.getJMSTimestamp();

      InstrumentationManager.instrumentWithChronometer(TransportManager.getTransport(), (timeInQueue >= 0) ? timeInQueue : 0, new MetricProperty("destination", MetricDestination.REQUEST_QUEUE.getDisplay()));
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
      LoggerManager.getLogger(ReceptionListener.class).error(exception);
    }
  }
}

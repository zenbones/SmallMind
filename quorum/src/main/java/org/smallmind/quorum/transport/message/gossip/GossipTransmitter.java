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

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.quorum.transport.message.ConnectionFactor;
import org.smallmind.quorum.transport.message.MessagePolicy;
import org.smallmind.quorum.transport.message.MessageProperty;
import org.smallmind.quorum.transport.message.MessageStrategy;
import org.smallmind.quorum.transport.message.ReconnectionPolicy;
import org.smallmind.quorum.transport.message.TopicOperator;
import org.smallmind.quorum.transport.message.TransportManagedObjects;

public class GossipTransmitter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MessageStrategy messageStrategy;
  private final LinkedBlockingQueue<TopicOperator> operatorQueue;
  private final ConnectionFactor[] gossipConnectionFactors;

  public GossipTransmitter (TransportManagedObjects gossipManagedObjects, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, MessageStrategy messageStrategy, int clusterSize, int concurrencyLimit)
    throws IOException, JMSException, TransportException {

    int gossipIndex = 0;

    this.messageStrategy = messageStrategy;

    gossipConnectionFactors = new ConnectionFactor[clusterSize];
    for (int index = 0; index < gossipConnectionFactors.length; index++) {
      gossipConnectionFactors[index] = new ConnectionFactor(gossipManagedObjects, messagePolicy, reconnectionPolicy);
    }

    operatorQueue = new LinkedBlockingQueue<TopicOperator>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      operatorQueue.add(new TopicOperator(gossipConnectionFactors[gossipIndex], (Topic)gossipManagedObjects.getDestination()));
      if (++gossipIndex == gossipConnectionFactors.length) {
        gossipIndex = 0;
      }
    }
  }

  public void sendMessage (final InvocationSignal invocationSignal, final String serviceSelector)
    throws Exception {

    final TopicOperator topicOperator;

    topicOperator = InstrumentationManager.execute(new ChronometerInstrumentAndReturn<TopicOperator>(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.ACQUIRE_TOPIC.getDisplay())) {

      @Override
      public TopicOperator withChronometer ()
        throws TransportException, InterruptedException {

        TopicOperator topicOperator;

        do {
          topicOperator = operatorQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (topicOperator == null));

        if (topicOperator == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return topicOperator;
      }
    });

    try {

      Message gossipMessage;

      gossipMessage = InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.CONSTRUCT_MESSAGE.getDisplay())) {

        @Override
        public Message withChronometer ()
          throws Exception {

          Message gossipMessage;

          gossipMessage = messageStrategy.wrapInMessage(topicOperator.getTopicSession(), invocationSignal);
          gossipMessage.setStringProperty(MessageProperty.SERVICE.getKey(), serviceSelector);

          return gossipMessage;
        }
      });

      topicOperator.publish(gossipMessage);
    }
    finally {
      operatorQueue.put(topicOperator);
    }
  }

  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (ConnectionFactor gossipConnectionFactor : gossipConnectionFactors) {
        gossipConnectionFactor.stop();
      }
      for (ConnectionFactor gossipConnectionFactor : gossipConnectionFactors) {
        gossipConnectionFactor.close();
      }
    }
  }
}

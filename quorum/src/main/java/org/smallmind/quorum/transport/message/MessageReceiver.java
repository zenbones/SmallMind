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

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import org.smallmind.quorum.transport.TransportException;

public class MessageReceiver {

  private static final Random RANDOM = new SecureRandom();

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<TopicOperator> operatorQueue;
  private final ReceptionListener[] receptionListeners;
  private final ReceptionWorker[] receptionWorkers;
  private final TopicConnection[] responseConnections;

  public MessageReceiver (TransportManagedObjects requestManagedObjects, TransportManagedObjects responseManagedObjects, MessagePolicy messagePolicy, MessageStrategy messageStrategy, int clusterSize, int concurrencyLimit, MessageTarget... messageTargets)
    throws JMSException, TransportException {

    SynchronousQueue<Message> messageRendezvous = new SynchronousQueue<Message>(true);
    HashMap<String, MessageTarget> targetMap = new HashMap<String, MessageTarget>();

    int topicIndex;

    for (MessageTarget messageTarget : messageTargets) {
      targetMap.put(messageTarget.getServiceInterface().getName(), messageTarget);
    }

    receptionListeners = new ReceptionListener[clusterSize];
    for (int index = 0; index < receptionListeners.length; index++) {
      receptionListeners[index] = new ReceptionListener((QueueConnection)requestManagedObjects.createConnection(), (Queue)requestManagedObjects.getDestination(), messagePolicy.getAcknowledgeMode(), messageRendezvous);
    }

    responseConnections = new TopicConnection[clusterSize];
    for (int index = 0; index < responseConnections.length; index++) {
      responseConnections[index] = (TopicConnection)responseManagedObjects.createConnection();
    }

    topicIndex = RANDOM.nextInt(responseConnections.length);

    operatorQueue = new ConcurrentLinkedQueue<TopicOperator>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      operatorQueue.add(new TopicOperator(responseConnections[topicIndex], (Topic)responseManagedObjects.getDestination(), messagePolicy));
      if (++topicIndex == responseConnections.length) {
        topicIndex = 0;
      }
    }

    receptionWorkers = new ReceptionWorker[concurrencyLimit];
    for (int index = 0; index < receptionWorkers.length; index++) {
      new Thread(receptionWorkers[index] = new ReceptionWorker(messageStrategy, targetMap, messageRendezvous, operatorQueue)).start();
    }
  }

  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (ReceptionListener receptionListener : receptionListeners) {
        receptionListener.close();
      }
      for (TopicConnection responseConnection : responseConnections) {
        responseConnection.stop();
      }
      for (TopicOperator topicOperator : operatorQueue) {
        topicOperator.close();
      }
      for (TopicConnection responseConnection : responseConnections) {
        responseConnection.close();
      }
      for (ReceptionWorker receptionWorker : receptionWorkers) {
        receptionWorker.stop();
      }
    }
  }
}


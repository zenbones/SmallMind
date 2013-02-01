/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011, 2012, 2013 David Berkman
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

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.TransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import org.smallmind.quorum.transport.TransportException;

public class MessageReceiver {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final ReceptionListener[] receptionListeners;
  private final ReceptionWorker[] receptionWorkers;
  private final ConnectionFactor[] responseConnectionFactors;

  public MessageReceiver (TransportManagedObjects requestManagedObjects, TransportManagedObjects responseManagedObjects, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, MessageStrategy messageStrategy, int clusterSize, int concurrencyLimit, MessageTarget... messageTargets)
    throws IOException, JMSException, TransportException {

    ConcurrentLinkedQueue<TopicOperator> operatorQueue;
    TransferQueue<Message> messageRendezvous = new LinkedTransferQueue<Message>();
    HashMap<String, MessageTarget> targetMap = new HashMap<String, MessageTarget>();
    int topicIndex = 0;

    for (MessageTarget messageTarget : messageTargets) {
      targetMap.put(messageTarget.getServiceInterface().getName(), messageTarget);
    }

    receptionListeners = new ReceptionListener[clusterSize];
    for (int index = 0; index < receptionListeners.length; index++) {
      receptionListeners[index] = new ReceptionListener(new ConnectionFactor(requestManagedObjects, messagePolicy, reconnectionPolicy), (Queue)requestManagedObjects.getDestination(), messageRendezvous);
    }

    responseConnectionFactors = new ConnectionFactor[clusterSize];
    for (int index = 0; index < responseConnectionFactors.length; index++) {
      responseConnectionFactors[index] = new ConnectionFactor(responseManagedObjects, messagePolicy, reconnectionPolicy);
    }

    operatorQueue = new ConcurrentLinkedQueue<TopicOperator>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      operatorQueue.add(new TopicOperator(responseConnectionFactors[topicIndex], (Topic)responseManagedObjects.getDestination()));
      if (++topicIndex == responseConnectionFactors.length) {
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
      for (ConnectionFactor responseConnectionFactor : responseConnectionFactors) {
        responseConnectionFactor.stop();
      }
      for (ReceptionWorker receptionWorker : receptionWorkers) {
        receptionWorker.stop();
      }
      for (ConnectionFactor responseConnectionFactor : responseConnectionFactors) {
        responseConnectionFactor.close();
      }
    }
  }
}


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
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageTransmitter {

  private static final Random RANDOM = new SecureRandom();

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MessageStrategy messageStrategy;
  private final LinkedBlockingQueue<QueueOperator> operatorQueue;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final TransmissionListener[] transmissionListeners;
  private final QueueConnection[] requestConnections;
  private final String instanceId = UUID.randomUUID().toString();

  public MessageTransmitter (TransportManagedObjects requestManagedObjects, TransportManagedObjects responseManagedObjects, MessagePolicy messagePolicy, MessageStrategy messageStrategy, int clusterSize, int concurrencyLimit, int timeoutSeconds)
    throws JMSException, TransportException {

    int requestIndex;

    this.messageStrategy = messageStrategy;

    callbackMap = new SelfDestructiveMap<String, TransmissionCallback>(timeoutSeconds);

    requestConnections = new QueueConnection[clusterSize];
    for (int index = 0; index < requestConnections.length; index++) {
      requestConnections[index] = (QueueConnection)requestManagedObjects.createConnection();
    }

    requestIndex = RANDOM.nextInt(requestConnections.length);

    operatorQueue = new LinkedBlockingQueue<QueueOperator>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      operatorQueue.add(new QueueOperator(requestConnections[requestIndex], (Queue)requestManagedObjects.getDestination(), messagePolicy));
      if (++requestIndex == requestConnections.length) {
        requestIndex = 0;
      }
    }

    transmissionListeners = new TransmissionListener[clusterSize];
    for (int index = 0; index < transmissionListeners.length; index++) {
      transmissionListeners[index] = new TransmissionListener(this, (TopicConnection)responseManagedObjects.createConnection(), (Topic)responseManagedObjects.getDestination(), messagePolicy.getAcknowledgeMode());
    }
  }

  public String getInstanceId () {

    return instanceId;
  }

  public TransmissionCallback sendMessage (InvocationSignal invocationSignal, String serviceSelector)
    throws Exception {

    QueueOperator queueOperator;

    do {
      queueOperator = operatorQueue.poll(1, TimeUnit.SECONDS);
    } while ((!closed.get()) && (queueOperator == null));

    if (queueOperator == null) {
      throw new TransportException("Message transmission has been closed");
    }

    try {

      Message requestMessage = messageStrategy.wrapInMessage(queueOperator.getRequestSession(), invocationSignal);
      AsynchronousTransmissionCallback asynchronousCallback;
      SynchronousTransmissionCallback previousCallback;

      requestMessage.setStringProperty(MessageProperty.INSTANCE.getKey(), instanceId);
      requestMessage.setStringProperty(MessageProperty.SERVICE.getKey(), serviceSelector);

      queueOperator.send(requestMessage);
      if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(requestMessage.getJMSMessageID(), asynchronousCallback = new AsynchronousTransmissionCallback(messageStrategy))) != null) {

        return previousCallback;
      }

      return asynchronousCallback;
    }
    finally {
      operatorQueue.put(queueOperator);
    }
  }

  public void completeCallback (Message responseMessage) {

    try {

      AsynchronousTransmissionCallback previousCallback;
      String correlationId;

      if ((previousCallback = (AsynchronousTransmissionCallback)callbackMap.get(correlationId = responseMessage.getJMSCorrelationID())) == null) {
        if ((previousCallback = (AsynchronousTransmissionCallback)callbackMap.putIfAbsent(correlationId, new SynchronousTransmissionCallback(messageStrategy, responseMessage))) != null) {
          previousCallback.setResponseMessage(responseMessage);
        }
      }
      else {
        previousCallback.setResponseMessage(responseMessage);
      }
    }
    catch (JMSException jmsException) {
      LoggerManager.getLogger(MessageTransmitter.class).error(jmsException);
    }
  }

  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (QueueConnection requestConnection : requestConnections) {
        requestConnection.stop();
      }
      for (QueueOperator queueOperator : operatorQueue) {
        queueOperator.close();
      }
      for (QueueConnection requestConnection : requestConnections) {
        requestConnection.close();
      }
      for (TransmissionListener transmissionListener : transmissionListeners) {
        transmissionListener.close();
      }
      callbackMap.shutdown();
    }
  }
}

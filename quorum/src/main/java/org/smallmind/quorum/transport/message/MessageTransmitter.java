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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import org.smallmind.instrument.ChronometerInstrumentAndReturn;
import org.smallmind.instrument.InstrumentationManager;
import org.smallmind.instrument.MetricProperty;
import org.smallmind.nutsnbolts.ntp.NTPTime;
import org.smallmind.nutsnbolts.util.SelfDestructiveMap;
import org.smallmind.quorum.transport.InvocationSignal;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.quorum.transport.TransportManager;
import org.smallmind.quorum.transport.instrument.MetricEvent;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageTransmitter {

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final MessageStrategy messageStrategy;
  private final LinkedBlockingQueue<QueueOperator> operatorQueue;
  private final SelfDestructiveMap<String, TransmissionCallback> callbackMap;
  private final TransmissionListener[] transmissionListeners;
  private final ConnectionFactor[] requestConnectionFactors;
  private final String instanceId = UUID.randomUUID().toString();
  private final long ntpOffset;
  private final long timeoutSeconds;

  public MessageTransmitter (TransportManagedObjects requestManagedObjects, TransportManagedObjects responseManagedObjects, MessagePolicy messagePolicy, ReconnectionPolicy reconnectionPolicy, MessageStrategy messageStrategy, NTPTime ntpTime, int clusterSize, int concurrencyLimit, int timeoutSeconds)
    throws IOException, JMSException, TransportException {

    int requestIndex = 0;

    this.messageStrategy = messageStrategy;
    this.timeoutSeconds = timeoutSeconds;

    ntpOffset = (ntpTime == null) ? 0 : ntpTime.getOffset(10000);

    callbackMap = new SelfDestructiveMap<String, TransmissionCallback>(timeoutSeconds);

    requestConnectionFactors = new ConnectionFactor[clusterSize];
    for (int index = 0; index < requestConnectionFactors.length; index++) {
      requestConnectionFactors[index] = new ConnectionFactor(requestManagedObjects, messagePolicy, reconnectionPolicy);
    }

    operatorQueue = new LinkedBlockingQueue<QueueOperator>();
    for (int index = 0; index < Math.max(clusterSize, concurrencyLimit); index++) {
      operatorQueue.add(new QueueOperator(requestConnectionFactors[requestIndex], (Queue)requestManagedObjects.getDestination()));
      if (++requestIndex == requestConnectionFactors.length) {
        requestIndex = 0;
      }
    }

    transmissionListeners = new TransmissionListener[clusterSize];
    for (int index = 0; index < transmissionListeners.length; index++) {
      transmissionListeners[index] = new TransmissionListener(this, new ConnectionFactor(responseManagedObjects, messagePolicy, reconnectionPolicy), (Topic)responseManagedObjects.getDestination(), ntpOffset);
    }
  }

  public String getInstanceId () {

    return instanceId;
  }

  public TransmissionCallback sendMessage (final InvocationSignal invocationSignal, final String serviceSelector)
    throws Exception {

    final QueueOperator queueOperator;

    queueOperator = InstrumentationManager.execute(new ChronometerInstrumentAndReturn<QueueOperator>(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.ACQUIRE_QUEUE.getDisplay())) {

      @Override
      public QueueOperator withChronometer ()
        throws TransportException, InterruptedException {

        QueueOperator queueOperator;

        do {
          queueOperator = operatorQueue.poll(1, TimeUnit.SECONDS);
        } while ((!closed.get()) && (queueOperator == null));

        if (queueOperator == null) {
          throw new TransportException("Message transmission has been closed");
        }

        return queueOperator;
      }
    });

    try {

      Message requestMessage;
      AsynchronousTransmissionCallback asynchronousCallback;
      SynchronousTransmissionCallback previousCallback;

      requestMessage = InstrumentationManager.execute(new ChronometerInstrumentAndReturn<Message>(TransportManager.getTransport(), new MetricProperty("event", MetricEvent.CONSTRUCT_MESSAGE.getDisplay())) {

        @Override
        public Message withChronometer ()
          throws Exception {

          Message requestMessage;

          requestMessage = messageStrategy.wrapInMessage(queueOperator.getRequestSession(), invocationSignal);

          requestMessage.setStringProperty(MessageProperty.INSTANCE.getKey(), instanceId);
          requestMessage.setStringProperty(MessageProperty.SERVICE.getKey(), serviceSelector);
          requestMessage.setLongProperty(MessageProperty.TIME.getKey(), System.currentTimeMillis() + ntpOffset);

          return requestMessage;
        }
      });

      queueOperator.send(requestMessage);
      if ((previousCallback = (SynchronousTransmissionCallback)callbackMap.putIfAbsent(requestMessage.getJMSMessageID(), asynchronousCallback = new AsynchronousTransmissionCallback(messageStrategy, timeoutSeconds))) != null) {

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

      TransmissionCallback previousCallback;
      String correlationId;

      if ((previousCallback = callbackMap.get(correlationId = responseMessage.getJMSCorrelationID())) == null) {
        if ((previousCallback = callbackMap.putIfAbsent(correlationId, new SynchronousTransmissionCallback(messageStrategy, responseMessage))) != null) {
          if (previousCallback instanceof AsynchronousTransmissionCallback) {
            ((AsynchronousTransmissionCallback)previousCallback).setResponseMessage(responseMessage);
          }
        }
      }
      else {
        if (previousCallback instanceof AsynchronousTransmissionCallback) {
          ((AsynchronousTransmissionCallback)previousCallback).setResponseMessage(responseMessage);
        }
      }
    }
    catch (JMSException jmsException) {
      LoggerManager.getLogger(MessageTransmitter.class).error(jmsException);
    }
  }

  public void close ()
    throws JMSException, InterruptedException {

    if (closed.compareAndSet(false, true)) {
      for (ConnectionFactor requestConnectionFactor : requestConnectionFactors) {
        requestConnectionFactor.stop();
      }
      for (ConnectionFactor requestConnectionFactor : requestConnectionFactors) {
        requestConnectionFactor.close();
      }
      for (TransmissionListener transmissionListener : transmissionListeners) {
        transmissionListener.close();
      }
      callbackMap.shutdown();
    }
  }
}

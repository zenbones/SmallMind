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

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.Message;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class ReceptionWorker implements Runnable {

  private final AtomicBoolean stopped = new AtomicBoolean(false);
  private final CountDownLatch exitLatch = new CountDownLatch(1);
  private final MessageStrategy messageStrategy;
  private final Map<String, MessageTarget> targetMap;
  private final SynchronousQueue<Message> messageRendezvous;
  private final ConcurrentLinkedQueue<TopicOperator> operatorQueue;

  public ReceptionWorker (MessageStrategy messageStrategy, Map<String, MessageTarget> targetMap, SynchronousQueue<Message> messageRendezvous, ConcurrentLinkedQueue<TopicOperator> operatorQueue) {

    this.messageStrategy = messageStrategy;
    this.targetMap = targetMap;
    this.messageRendezvous = messageRendezvous;
    this.operatorQueue = operatorQueue;
  }

  public void stop ()
    throws InterruptedException {

    stopped.set(true);
    exitLatch.await();
  }

  @Override
  public void run () {

    try {
      while (!stopped.get()) {

        Message requestMessage;

        if ((requestMessage = messageRendezvous.poll(1, TimeUnit.SECONDS)) != null) {

          TopicOperator topicOperator;

          if ((topicOperator = operatorQueue.poll()) == null) {
            throw new TransportException("Unable to take a TopicOperator, which should never happen - please contact your system administrator");
          }

          try {

            Message responseMessage;
            String transmissionInstance;

            if ((transmissionInstance = requestMessage.getStringProperty(MessageProperty.INSTANCE.getKey())) == null) {
              throw new TransportException("Missing message property(%s)", MessageProperty.INSTANCE.getKey());
            }

            try {

              MessageTarget messageTarget;
              String serviceSelector;

              if ((serviceSelector = requestMessage.getStringProperty(MessageProperty.SERVICE.getKey())) == null) {
                throw new TransportException("Missing message property(%s)", MessageProperty.SERVICE.getKey());
              }
              else if ((messageTarget = targetMap.get(serviceSelector)) == null) {
                throw new TransportException("Unknown service selector(%s)", serviceSelector);
              }

              responseMessage = messageTarget.handleMessage(topicOperator.getResponseSession(), messageStrategy, requestMessage);
            }
            catch (Exception exception) {
              responseMessage = messageStrategy.wrapInMessage(topicOperator.getResponseSession(), exception);
              responseMessage.setBooleanProperty(MessageProperty.EXCEPTION.getKey(), true);
            }

            responseMessage.setJMSCorrelationID(requestMessage.getJMSMessageID());
            responseMessage.setStringProperty(MessageProperty.INSTANCE.getKey(), transmissionInstance);

            topicOperator.publish(responseMessage);
          }
          catch (Throwable throwable) {
            LoggerManager.getLogger(ReceptionWorker.class).error(throwable);
          }
          finally {
            operatorQueue.add(topicOperator);
          }
        }
      }
    }
    catch (Exception exception) {
      LoggerManager.getLogger(ReceptionWorker.class).error(exception);
    }
    finally {
      exitLatch.countDown();
    }
  }
}

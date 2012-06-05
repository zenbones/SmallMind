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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import org.smallmind.quorum.transport.TransportException;
import org.smallmind.scribe.pen.LoggerManager;

public class MessageDistributor implements MessageListener, Runnable {

  private final QueueSenderLRUCache queueSenderLRUCache;

  private CountDownLatch exitLatch;
  private AtomicBoolean stopped = new AtomicBoolean(false);
  private QueueSession queueSession;
  private QueueReceiver queueReceiver;
  private MessagePolicy messagePolicy;
  private MessageStrategy messageStrategy;
  private Map<String, MessageTarget> targetMap;

  public MessageDistributor (QueueConnection queueConnection, Queue queue, MessagePolicy messagePolicy, MessageStrategy messageStrategy, Map<String, MessageTarget> targetMap, int replyCacheSize)
    throws TransportException, JMSException {

    this.messagePolicy = messagePolicy;
    this.messageStrategy = messageStrategy;
    this.targetMap = targetMap;

    queueSenderLRUCache = new QueueSenderLRUCache(replyCacheSize);

    queueSession = queueConnection.createQueueSession(false, messagePolicy.getAcknowledgeMode().getJmsValue());

    queueReceiver = queueSession.createReceiver(queue);
    queueReceiver.setMessageListener(this);
    queueConnection.start();

    exitLatch = new CountDownLatch(1);
  }

  public void close ()
    throws JMSException {

    if (stopped.compareAndSet(false, true)) {
      try {
        queueReceiver.close();

        for (QueueSender queueSender : queueSenderLRUCache.values()) {
          queueSender.close();
        }

        queueSession.close();
      }
      finally {
        exitLatch.countDown();
      }
    }
  }

  @Override
  public void run () {

    try {
      exitLatch.await();
    }
    catch (InterruptedException interruptedException) {
      LoggerManager.getLogger(MessageDistributor.class).error(interruptedException);
    }
  }

  public synchronized void onMessage (Message message) {

    try {

      Message responseMessage;
      QueueSender queueSender;
      Queue replyQueue;
      String replyQueueName;

      try {

        MessageTarget messageTarget;
        String serviceSelector;

        if ((serviceSelector = message.getStringProperty(MessageProperty.SERVICE.getKey())) == null) {
          throw new TransportException("Missing message property(%s)", MessageProperty.SERVICE.getKey());
        }
        else if ((messageTarget = targetMap.get(serviceSelector)) == null) {
          throw new TransportException("Unknown service selector(%s)", serviceSelector);
        }

        responseMessage = messageTarget.handleMessage(queueSession, messageStrategy, message);
      }
      catch (Exception exception) {
        LoggerManager.getLogger(MessageDistributor.class).error(exception);

        responseMessage = messageStrategy.wrapInMessage(queueSession, exception);
        responseMessage.setBooleanProperty(MessageProperty.EXCEPTION.getKey(), true);
      }

      if ((queueSender = queueSenderLRUCache.get(replyQueueName = (replyQueue = (Queue)message.getJMSReplyTo()).getQueueName())) == null) {
        queueSenderLRUCache.put(replyQueueName, queueSender = queueSession.createSender(replyQueue));
      }

      messagePolicy.apply(queueSender);
      queueSender.send(responseMessage);
    }
    catch (Throwable throwable) {
      LoggerManager.getLogger(MessageDistributor.class).error(throwable);
    }
  }
}

/*
 * Copyright (c) 2007, 2008, 2009, 2010, 2011 David Berkman
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
package org.smallmind.quorum.transport.messaging;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.naming.Context;
import javax.naming.NamingException;
import org.smallmind.quorum.pool2.ComponentFactory;
import org.smallmind.quorum.pool2.ComponentPool;
import org.smallmind.quorum.pool2.ComponentPoolException;
import org.smallmind.quorum.pool2.ConnectionPoolException;

public class MessagingTransmitter {

  private QueueConnection queueConnection;
  private QueueSession queueSession;
  private QueueSender queueSender;
  private ComponentPool<MessageSender> messageSenderPool;
  private String serviceSelector;

  public MessagingTransmitter (MessagingConnectionDetails messagingConnectionDetails)
    throws ConnectionPoolException, NamingException, JMSException {

    Context javaEnvironment;
    Queue queue;
    QueueConnectionFactory queueConnectionFactory;

    javaEnvironment = (Context)messagingConnectionDetails.getContextPool().getConnection();
    try {
      queue = (Queue)javaEnvironment.lookup(messagingConnectionDetails.getDestinationName());
      queueConnectionFactory = (QueueConnectionFactory)javaEnvironment.lookup(messagingConnectionDetails.getConnectionFactoryName());
    }
    finally {
      javaEnvironment.close();
    }

    serviceSelector = messagingConnectionDetails.getServiceSelector();

    queueConnection = queueConnectionFactory.createQueueConnection(messagingConnectionDetails.getUserName(), messagingConnectionDetails.getPassword());
    queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    queueSender = queueSession.createSender(queue);

    messageSenderPool = new ComponentPool<MessageSender>(new MessageSenderComponentFactory(this), messagingConnectionDetails.getTransmissionPoolSize(), 0);

    queueConnection.start();
  }

  public MessageSender borrowMessageSender ()
    throws ComponentPoolException {

    return messageSenderPool.getComponent();
  }

  public void returnMessageSender (MessageSender messageSender) {

    messageSenderPool.returnComponent(messageSender);
  }

  public ObjectMessage createObjectMessage (Serializable serializable)
    throws JMSException {

    return queueSession.createObjectMessage(serializable);
  }

  public void sendMessage (TemporaryQueue temporaryQueue, Message message)
    throws JMSException {

    if (serviceSelector != null) {
      message.setStringProperty(MessagingConnectionDetails.SELECTION_PROPERTY, serviceSelector);
    }

    message.setJMSReplyTo(temporaryQueue);
    queueSender.send(message);
  }

  public Object getResult (QueueReceiver queueReceiver)
    throws JMSException, InvocationTargetException {

    ObjectMessage objectMessage;

    objectMessage = (ObjectMessage)queueReceiver.receive();
    if (objectMessage.getBooleanProperty(MessagingConnectionDetails.EXCEPTION_PROPERTY)) {
      throw new InvocationTargetException((Exception)objectMessage.getObject());
    }

    return objectMessage.getObject();
  }

  public void close ()
    throws JMSException {

    queueConnection.stop();

    queueSender.close();
    queueSession.close();
    queueConnection.close();
  }

  public void finalize ()
    throws JMSException {

    close();
  }

  private class MessageSenderComponentFactory implements ComponentFactory<MessageSender> {

    private MessagingTransmitter messagingTransmitter;

    public MessageSenderComponentFactory (MessagingTransmitter messagingTransmitter) {

      this.messagingTransmitter = messagingTransmitter;
    }

    public MessageSender createComponent ()
      throws JMSException {

      TemporaryQueue temporaryQueue = queueSession.createTemporaryQueue();

      return new MessageSender(messagingTransmitter, temporaryQueue, queueSession.createReceiver(temporaryQueue));
    }
  }
}

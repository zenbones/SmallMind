package org.smallmind.cloud.transport.messaging;

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
import org.smallmind.quorum.pool.ComponentFactory;
import org.smallmind.quorum.pool.ComponentPool;
import org.smallmind.quorum.pool.ComponentPoolException;
import org.smallmind.quorum.pool.ConnectionPoolException;
import org.smallmind.quorum.pool.PoolMode;

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

      messageSenderPool = new ComponentPool<MessageSender>(new MessageSenderComponentFactory(this), messagingConnectionDetails.getTransmissionPoolSize(), PoolMode.BLOCKING_POOL);

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

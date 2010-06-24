package org.smallmind.cloud.transport.messaging;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.QueueReceiver;
import javax.jms.TemporaryQueue;

public class MessageSender {

   private MessagingTransmitter messagingTransmitter;
   private TemporaryQueue temporaryQueue;
   private QueueReceiver queueReceiver;

   public MessageSender (MessagingTransmitter messagingTransmitter, TemporaryQueue temporaryQueue, QueueReceiver queueReceiver) {

      this.messagingTransmitter = messagingTransmitter;
      this.temporaryQueue = temporaryQueue;
      this.queueReceiver = queueReceiver;
   }

   public ObjectMessage createObjectMessage (Serializable serializable)
      throws JMSException {

      return messagingTransmitter.createObjectMessage(serializable);
   }

   public void sendMessage (Message message)
      throws JMSException {

      messagingTransmitter.sendMessage(temporaryQueue, message);
   }

   public Object getResult ()
      throws JMSException, InvocationTargetException {

      return messagingTransmitter.getResult(queueReceiver);
   }

   public Message recieveMessage ()
      throws JMSException {

      return queueReceiver.receive();
   }

   public void close ()
      throws JMSException {

      queueReceiver.close();
      temporaryQueue.delete();
   }

   public void finalize ()
      throws JMSException {

      close();
   }
}

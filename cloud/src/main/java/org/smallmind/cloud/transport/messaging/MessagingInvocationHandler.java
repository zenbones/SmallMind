package org.smallmind.cloud.transport.messaging;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.cloud.transport.FauxMethod;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class MessagingInvocationHandler implements InvocationHandler {

   private MessagingTransmitter messagingTransmitter;
   private Class invocableInterface;

   public MessagingInvocationHandler (MessagingTransmitter messagingTransmitter, Class invocableInterface) {

      this.messagingTransmitter = messagingTransmitter;
      this.invocableInterface = invocableInterface;
   }

   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      MessageSender messageSender;

      messageSender = messagingTransmitter.borrowMessageSender();

      try {
         messageSender.sendMessage(messageSender.createObjectMessage(new InvocationSignal(ContextFactory.getExpectedContexts(invocableInterface), new FauxMethod(method), args)));
         return messageSender.getResult();
      }
      finally {
         messagingTransmitter.returnMessageSender(messageSender);
      }
   }
}
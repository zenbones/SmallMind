package org.smallmind.cloud.transport.messaging;

import java.lang.reflect.Proxy;

public class InvocationProxyFactory {

   public static Proxy generateProxy (MessagingTransmitter messagingTransmitter, Class invocableInterface) {

      return (Proxy)Proxy.newProxyInstance(invocableInterface.getClassLoader(), new Class[] {invocableInterface}, new MessagingInvocationHandler(messagingTransmitter, invocableInterface));
   }
}

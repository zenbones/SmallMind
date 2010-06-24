package org.smallmind.cloud.transport.messaging.service;

import javax.jms.JMSException;
import org.smallmind.cloud.transport.messaging.InvocationProxyFactory;
import org.smallmind.cloud.transport.messaging.MessagingTransmitter;

public class ServiceHandleFactory {

   public static <S> S createServiceHandle (MessagingTransmitter messagingTransmitter, Class<S> serviceInterface)
      throws JMSException {

      return serviceInterface.cast(InvocationProxyFactory.generateProxy(messagingTransmitter, serviceInterface));
   }
}
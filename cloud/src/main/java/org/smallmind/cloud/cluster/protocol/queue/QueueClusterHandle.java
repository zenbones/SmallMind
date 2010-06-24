package org.smallmind.cloud.cluster.protocol.queue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.transport.messaging.MessagingInvocationHandler;

public class QueueClusterHandle implements ClusterHandle, InvocationHandler {

   private MessagingInvocationHandler messagingInvocationHandler;

   public QueueClusterHandle (MessagingInvocationHandler messagingInvocationHandlerr) {

      this.messagingInvocationHandler = messagingInvocationHandlerr;
   }

   public Object invoke (Object proxy, Method method, Object[] args) throws Throwable {

      return messagingInvocationHandler.invoke(proxy, method, args);
   }
}

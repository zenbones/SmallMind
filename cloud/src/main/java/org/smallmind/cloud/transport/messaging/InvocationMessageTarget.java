package org.smallmind.cloud.transport.messaging;

import java.io.Serializable;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.cloud.transport.MethodInvoker;

public abstract class InvocationMessageTarget implements MessageTarget {

   private MethodInvoker methodInvoker;

   public InvocationMessageTarget (Object targetObject, Class serviceInterface)
      throws NoSuchMethodException {

      methodInvoker = new MethodInvoker(targetObject, new Class[] {serviceInterface});
   }

   public Message handleMessage (Session session, Message message)
      throws Exception {

      InvocationSignal invocationSignal;
      Serializable result;

      invocationSignal = (InvocationSignal)((ObjectMessage)message).getObject();
      result = (Serializable)methodInvoker.remoteInvocation(invocationSignal);
      return session.createObjectMessage(result);
   }
}

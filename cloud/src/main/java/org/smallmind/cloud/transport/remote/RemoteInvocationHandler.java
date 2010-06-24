package org.smallmind.cloud.transport.remote;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import javax.naming.NamingException;
import org.smallmind.cloud.transport.FauxMethod;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class RemoteInvocationHandler implements Serializable, InvocationHandler {

   private Class endpointInterface;
   private RemoteTarget remoteTarget;

   public RemoteInvocationHandler (Class endpointInterface, RemoteTarget remoteTarget)
      throws NamingException {

      this.endpointInterface = endpointInterface;
      this.remoteTarget = remoteTarget;
   }

   public Object invoke (Object proxy, Method method, Object[] args)
      throws Throwable {

      return remoteTarget.remoteInvocation(new InvocationSignal(ContextFactory.getExpectedContexts(endpointInterface), new FauxMethod(method), args));
   }
}

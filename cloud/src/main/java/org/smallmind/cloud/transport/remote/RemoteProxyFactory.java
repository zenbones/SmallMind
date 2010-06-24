package org.smallmind.cloud.transport.remote;

import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

public class RemoteProxyFactory {

   public static Proxy generateRemoteProxy (Class endpointInterface, String hostName, String registryName)
      throws NoSuchMethodException, NamingException, RemoteException {

      RemoteTarget remoteTarget;
      InitialContext initContext;
      Context rmiContext;

      initContext = new InitialContext();
      rmiContext = (Context)initContext.lookup("rmi://" + hostName);
      remoteTarget = (RemoteTarget)PortableRemoteObject.narrow(rmiContext.lookup(registryName), RemoteTarget.class);
      rmiContext.close();
      initContext.close();

      return (Proxy)Proxy.newProxyInstance(RemoteInvocationHandler.class.getClassLoader(), new Class[] {endpointInterface}, new RemoteInvocationHandler(endpointInterface, remoteTarget));
   }
}

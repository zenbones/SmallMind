package org.smallmind.cloud.transport.remote;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.cloud.transport.MethodInvoker;

public class RemoteTargetImpl extends UnicastRemoteObject implements RemoteTarget {

   private MethodInvoker methodInvoker;

   public RemoteTargetImpl (RemoteEndpoint remoteEndpoint, String registryName)
      throws NoSuchMethodException, MalformedURLException, RemoteException {

      Naming.rebind(registryName, this);
      methodInvoker = new MethodInvoker(remoteEndpoint, remoteEndpoint.getProxyInterfaces());
   }

   public Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception {

      return methodInvoker.remoteInvocation(invocationSignal);
   }
}

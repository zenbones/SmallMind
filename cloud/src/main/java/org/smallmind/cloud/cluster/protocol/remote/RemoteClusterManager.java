package org.smallmind.cloud.cluster.protocol.remote;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.RemoteException;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import org.smallmind.cloud.cluster.ClusterEndpoint;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManager;
import org.smallmind.cloud.transport.FauxMethod;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.cloud.transport.MissingInvocationException;
import org.smallmind.nutsnbolts.context.ContextFactory;

public class RemoteClusterManager implements ClusterManager<RemoteClusterProtocolDetails> {

   private static final String RMI_NAME_PREFIX = "org.smallmind.cloud.cluster.protocol.remote.";

   private final HashMap<ClusterEndpoint, RemoteClusterService> rmiServerMap;

   private ClusterHub clusterHub;
   private Proxy clusterProxy;
   private ClusterInterface<RemoteClusterProtocolDetails> clusterInterface;

   protected RemoteClusterManager (ClusterHub clusterHub, ClusterInterface<RemoteClusterProtocolDetails> clusterInterface) {

      RemoteClusterHandle clusterHandle;

      this.clusterHub = clusterHub;
      this.clusterInterface = clusterInterface;

      rmiServerMap = new HashMap<ClusterEndpoint, RemoteClusterService>();

      clusterHandle = new RemoteClusterHandle(this);
      clusterProxy = (Proxy)Proxy.newProxyInstance(clusterInterface.getClusterProtocolDetails().getServiceInterface().getClassLoader(), new Class[] {ClusterHandle.class, clusterInterface.getClusterProtocolDetails().getServiceInterface()}, clusterHandle);
   }

   public ClusterInterface<RemoteClusterProtocolDetails> getClusterInterface () {

      return clusterInterface;
   }

   public ClusterHandle getClusterHandle () {

      return (ClusterHandle)clusterProxy;
   }

   public void updateClusterStatus (ClusterEndpoint clusterEndpoint, int calibratedFreeCapacity)
      throws ClusterManagementException {

      InitialContext initContext;
      Context rmiContext;
      RemoteClusterService remoteHandle;

      synchronized (rmiServerMap) {
         if (!rmiServerMap.containsKey(clusterEndpoint)) {
            try {
               initContext = new InitialContext();
               rmiContext = (Context)initContext.lookup("rmi://" + clusterEndpoint.getHostName());
               remoteHandle = (RemoteClusterService)PortableRemoteObject.narrow(rmiContext.lookup(RMI_NAME_PREFIX + clusterInterface.getClusterName() + ".instance." + clusterEndpoint.getClusterInstance().getInstanceId()), RemoteClusterService.class);
               rmiContext.close();
               initContext.close();
            }
            catch (NamingException namingException) {
               throw new ClusterManagementException(namingException);
            }

            rmiServerMap.put(clusterEndpoint, remoteHandle);
         }
      }

      clusterInterface.getClusterPivot().updateClusterStatus(clusterEndpoint, calibratedFreeCapacity);
   }

   public void removeClusterMember (ClusterEndpoint clusterEndpoint) {

      synchronized (rmiServerMap) {
         if (rmiServerMap.containsKey(clusterEndpoint)) {
            rmiServerMap.remove(clusterEndpoint);
            clusterInterface.getClusterPivot().removeClusterMember(clusterEndpoint);
         }
      }
   }

   public Object invoke (Method method, Object[] args)
      throws Exception {

      Object[] pivotParameters;
      ClusterEndpoint clusterEndpoint = null;
      RemoteClusterService remoteHandle;

      if (args == null) {
         pivotParameters = new Object[1];
      }
      else {
         pivotParameters = new Object[args.length + 1];
      }

      pivotParameters[0] = method;
      if (args != null) {
         System.arraycopy(args, 0, pivotParameters, 1, args.length);
      }

      while (true) {
         synchronized (rmiServerMap) {
            if ((clusterEndpoint = clusterInterface.getClusterPivot().nextRequestAddress(pivotParameters, clusterEndpoint)) == null) {
               throw new ClusterManagementException("No server is currently available for requests to ClusterInterface (%s)", clusterInterface);
            }
            remoteHandle = rmiServerMap.get(clusterEndpoint);
         }

         if (remoteHandle != null) {
            try {
               return remoteHandle.remoteInvocation(new InvocationSignal(ContextFactory.getExpectedContexts(clusterInterface.getClusterProtocolDetails().getServiceInterface()), new FauxMethod(method), args));
            }
            catch (MissingInvocationException missingInvocationException) {
               throw new ClusterManagementException("Could not invoke method (%s) on the remote cluster", method.getName());
            }
            catch (RemoteException remoteException) {
               clusterHub.logError(remoteException);
               removeClusterMember(clusterEndpoint);
            }
         }
         else {
            clusterHub.logError(new ClusterManagementException("Pivot ClusterEndpoint/remote handle mismatch on cluster (%s)", clusterEndpoint));
            removeClusterMember(clusterEndpoint);
         }
      }
   }
}

package org.smallmind.cloud.cluster.protocol.remote;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterMember;
import org.smallmind.cloud.cluster.ClusterService;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.cloud.transport.InvocationSignal;
import org.smallmind.cloud.transport.MethodInvoker;

public class RemoteClusterServiceImpl extends UnicastRemoteObject implements RemoteClusterService, ClusterService<RemoteClusterProtocolDetails> {

   private static final String RMI_NAME_PREFIX = "org.smallmind.cloud.cluster.protocol.remote.";

   private ClusterHub clusterHub;
   private ClusterMember clusterMember = null;
   private ClusterInstance<RemoteClusterProtocolDetails> clusterInstance;
   private MethodInvoker methodInvoker;
   private boolean open = true;

   public RemoteClusterServiceImpl (ClusterHub clusterHub, ClusterInstance<RemoteClusterProtocolDetails> clusterInstance)
      throws RemoteException {

      this.clusterHub = clusterHub;
      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance<RemoteClusterProtocolDetails> getClusterInstance () {

      return clusterInstance;
   }

   public void bindClusterMember (ClusterMember clusterMember)
      throws ClusterManagementException {

      this.clusterMember = clusterMember;

      try {
         methodInvoker = new MethodInvoker(clusterMember, new Class[] {clusterInstance.getClusterInterface().getClusterProtocolDetails().getServiceInterface()});
      }
      catch (NoSuchMethodException noSuchMethodException) {
         throw new ClusterManagementException(noSuchMethodException);
      }
   }

   public Object remoteInvocation (InvocationSignal invocationSignal)
      throws Exception {

      if (methodInvoker == null) {
         throw new IllegalStateException("No ClusterMember(" + clusterInstance.getClusterInterface().getClusterProtocolDetails().getServiceInterface().getCanonicalName() + ") has been bound to this service representation");
      }

      return methodInvoker.remoteInvocation(invocationSignal);
   }

   public synchronized void fireServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast)
      throws EventMessageException {

      if (!open) {
         throw new IllegalStateException("The service has already been closed");
      }

      clusterHub.fireEvent(serviceClusterBroadcast);
   }

   public synchronized void handleServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast) {

      if (!open) {
         throw new IllegalStateException("The service has already been closed");
      }

      clusterMember.handleServiceBroadcast(serviceClusterBroadcast);
   }

   public synchronized void start ()
      throws ClusterManagementException {

      if (!open) {
         try {
            Naming.rebind(RMI_NAME_PREFIX + clusterInstance.getClusterInterface().getClusterName() + ".instance." + clusterInstance.getInstanceId(), this);
         }
         catch (Exception exception) {
            throw new ClusterManagementException(exception);
         }

         clusterHub.addClusterService(this);
         open = true;
      }
   }

   public synchronized void stop () {

      if (open) {
         open = false;
         clusterHub.removeClusterService(getClusterInstance());

         try {
            Naming.unbind(RMI_NAME_PREFIX + clusterInstance.getClusterInterface().getClusterName() + ".instance." + clusterInstance.getInstanceId());
         }
         catch (Exception exception) {
            clusterHub.logError(exception);
         }
      }
   }

}

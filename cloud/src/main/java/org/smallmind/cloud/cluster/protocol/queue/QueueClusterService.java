package org.smallmind.cloud.cluster.protocol.queue;

import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterMember;
import org.smallmind.cloud.cluster.ClusterService;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;
import org.smallmind.cloud.transport.messaging.MessageTarget;
import org.smallmind.cloud.transport.messaging.MessagingReceiver;

public class QueueClusterService implements ClusterService<QueueClusterProtocolDetails> {

   private ClusterHub clusterHub;
   private MessagingReceiver messagingReceiver;
   private ClusterMember clusterMember;
   private ClusterInstance<QueueClusterProtocolDetails> clusterInstance;
   private boolean open = true;

   public QueueClusterService (ClusterHub clusterHub, ClusterInstance<QueueClusterProtocolDetails> clusterInstance) {

      this.clusterHub = clusterHub;
      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance<QueueClusterProtocolDetails> getClusterInstance () {

      return clusterInstance;
   }

   public void bindClusterMember (ClusterMember clusterMember) {

      this.clusterMember = clusterMember;
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
            messagingReceiver = new MessagingReceiver((MessageTarget)clusterMember, clusterInstance.getClusterInterface().getClusterProtocolDetails().getConnectionDetails());
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
         messagingReceiver.close();
      }
   }
}

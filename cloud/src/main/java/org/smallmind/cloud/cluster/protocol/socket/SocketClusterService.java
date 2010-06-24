package org.smallmind.cloud.cluster.protocol.socket;

import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterMember;
import org.smallmind.cloud.cluster.ClusterService;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;

public class SocketClusterService implements ClusterService<SocketClusterProtocolDetails> {

   private ClusterHub clusterHub;
   private ClusterMember clusterMember;
   private ClusterInstance<SocketClusterProtocolDetails> clusterInstance;
   private boolean open = true;

   public SocketClusterService (ClusterHub clusterHub, ClusterInstance<SocketClusterProtocolDetails> clusterInstance) {

      this.clusterHub = clusterHub;
      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance<SocketClusterProtocolDetails> getClusterInstance () {

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

   public synchronized void start () {

      if (!open) {
         clusterHub.addClusterService(this);
         open = true;
      }
   }

   public synchronized void stop () {

      if (open) {
         open = false;
         clusterHub.removeClusterService(getClusterInstance());
      }
   }
}

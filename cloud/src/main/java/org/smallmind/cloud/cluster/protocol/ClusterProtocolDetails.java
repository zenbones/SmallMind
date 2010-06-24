package org.smallmind.cloud.cluster.protocol;

public abstract class ClusterProtocolDetails {

   private ClusterProtocol clusterProtocol;

   public ClusterProtocolDetails (ClusterProtocol clusterProtocol) {

      this.clusterProtocol = clusterProtocol;
   }

   public ClusterProtocol getClusterProtocol () {

      return clusterProtocol;
   }
}

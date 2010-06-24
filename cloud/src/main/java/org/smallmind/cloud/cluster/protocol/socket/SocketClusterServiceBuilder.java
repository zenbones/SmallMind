package org.smallmind.cloud.cluster.protocol.socket;

import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterServiceBuilder;

public class SocketClusterServiceBuilder implements ClusterServiceBuilder<SocketClusterProtocolDetails> {

   public SocketClusterService getClusterService (ClusterHub clusterHub, ClusterInstance<SocketClusterProtocolDetails> clusterInstance) {

      return new SocketClusterService(clusterHub, clusterInstance);
   }
}
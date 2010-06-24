package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public interface ClusterServiceBuilder<D extends ClusterProtocolDetails> {

   public abstract ClusterService<D> getClusterService (ClusterHub clusterHub, ClusterInstance<D> clusterInstance)
      throws ClusterManagementException;
}

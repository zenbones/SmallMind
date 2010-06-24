package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public interface ClusterManagerBuilder<D extends ClusterProtocolDetails> {

   public abstract ClusterManager<D> getClusterManager (ClusterHub clusterHub, ClusterInterface<D> clusterInterface)
      throws ClusterManagementException;

}

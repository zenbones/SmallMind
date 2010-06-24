package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;

public interface ClusterManager<D extends ClusterProtocolDetails> {

   public abstract ClusterInterface<D> getClusterInterface ();

   public abstract ClusterHandle getClusterHandle ()
      throws ClusterManagementException;

   public abstract void updateClusterStatus (ClusterEndpoint clusterEndpoint, int calibratedFreeCapacity)
      throws ClusterManagementException;

   public abstract void removeClusterMember (ClusterEndpoint clusterEndpoint)
      throws ClusterManagementException;

}

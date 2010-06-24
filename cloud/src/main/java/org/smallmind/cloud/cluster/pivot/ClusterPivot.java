package org.smallmind.cloud.cluster.pivot;

import org.smallmind.cloud.cluster.ClusterEndpoint;

public interface ClusterPivot {

   public abstract void updateClusterStatus (ClusterEndpoint clusterEndpoint, int calibratedFreeCapacity);

   public abstract void removeClusterMember (ClusterEndpoint clusterEndpoint);

   public abstract ClusterEndpoint nextRequestAddress (Object[] parameters, ClusterEndpoint failedEndpoint);

}

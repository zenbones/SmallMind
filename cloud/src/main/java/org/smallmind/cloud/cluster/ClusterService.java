package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.cluster.protocol.ClusterProtocolDetails;
import org.smallmind.cloud.multicast.EventMessageException;

public interface ClusterService<D extends ClusterProtocolDetails> {

   public ClusterInstance<D> getClusterInstance ();

   public abstract void bindClusterMember (ClusterMember clusterMember)
      throws ClusterManagementException;

   public abstract void handleServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast);

   public abstract void fireServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast)
      throws EventMessageException;

   public abstract void start ()
      throws ClusterManagementException;

   public abstract void stop ();

}

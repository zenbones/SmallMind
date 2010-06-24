package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.broadcast.GossipClusterBroadcast;
import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;
import org.smallmind.cloud.multicast.EventMessageException;

public interface
   ClusterMember {

   public abstract void handleServiceBroadcast (ServiceClusterBroadcast serviceClusterBroadcast);

   public abstract void fireGossipBroadcast (GossipClusterBroadcast gossipClusterBroadcast)
      throws EventMessageException;

   public abstract void logError (Class errorClass, Throwable throwable);

}

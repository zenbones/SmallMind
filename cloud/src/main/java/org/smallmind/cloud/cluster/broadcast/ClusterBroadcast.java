package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.multicast.event.MulticastEvent;

public class ClusterBroadcast extends MulticastEvent {

   private ClusterBroadcastType clusterBroadcastType;

   public ClusterBroadcast (ClusterBroadcastType clusterBroadcastType)
      throws UnknownHostException {

      super();

      this.clusterBroadcastType = clusterBroadcastType;
   }

   public ClusterBroadcastType getClusterBroadcastType () {

      return clusterBroadcastType;
   }

}

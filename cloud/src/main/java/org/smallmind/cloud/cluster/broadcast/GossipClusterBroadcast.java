package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.event.GossipClusterEvent;

public class GossipClusterBroadcast extends ClusterBroadcast {

   private ClusterInterface clusterInterface;
   private GossipClusterEvent gossipClusterEvent;

   public GossipClusterBroadcast (ClusterInterface clusterInterface, GossipClusterEvent gossipClusterEvnt)
      throws UnknownHostException {

      super(ClusterBroadcastType.GOSSIP);

      this.clusterInterface = clusterInterface;
      this.gossipClusterEvent = gossipClusterEvnt;
   }

   public ClusterInterface getClusterInsterface () {

      return clusterInterface;
   }

   public GossipClusterEvent getGossipClusterEvent () {

      return gossipClusterEvent;
   }

}

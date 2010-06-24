package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.event.GossipClusterEvent;
import org.smallmind.cloud.cluster.event.GossipClusterListener;

public class ClusterHubGossipDelivery implements Runnable {

   private GossipClusterListener gossipClusterListener;
   private GossipClusterEvent gossipClusterEvent;

   public ClusterHubGossipDelivery (GossipClusterListener gossipClusterListener, GossipClusterEvent gossipClusterEvent) {

      this.gossipClusterListener = gossipClusterListener;
      this.gossipClusterEvent = gossipClusterEvent;
   }

   public void run () {

      gossipClusterListener.handleGossip(gossipClusterEvent);
   }

}

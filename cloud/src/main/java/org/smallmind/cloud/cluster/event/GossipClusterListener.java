package org.smallmind.cloud.cluster.event;

import java.util.EventListener;

public interface GossipClusterListener extends EventListener {

   public abstract void handleGossip (GossipClusterEvent gossipClusterEvent);

}

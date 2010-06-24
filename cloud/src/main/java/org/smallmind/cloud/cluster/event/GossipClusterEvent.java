package org.smallmind.cloud.cluster.event;

import java.io.Serializable;
import org.smallmind.cloud.cluster.ClusterInstance;

public class GossipClusterEvent implements Serializable {

   private ClusterInstance clusterInstance;

   public GossipClusterEvent (ClusterInstance clusterInstance) {

      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance getClusterInstance () {

      return clusterInstance;
   }

}

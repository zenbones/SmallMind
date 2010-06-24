package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInstance;

public class SingleShotClusterBroadcast extends SystemClusterBroadcast {

   ClusterInstance clusterInstance;

   public SingleShotClusterBroadcast (ClusterInstance clusterInstance)
      throws UnknownHostException {

      super();

      this.clusterInstance = clusterInstance;
   }

   public ClusterInstance getClusterInstance () {

      return clusterInstance;
   }

}

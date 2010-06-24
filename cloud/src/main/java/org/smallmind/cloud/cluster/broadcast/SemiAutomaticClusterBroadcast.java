package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInterface;

public class SemiAutomaticClusterBroadcast extends SystemClusterBroadcast {

   ClusterInterface clusterInterface;

   public SemiAutomaticClusterBroadcast (ClusterInterface clusterInterface)
      throws UnknownHostException {

      super();

      this.clusterInterface = clusterInterface;
   }

   public ClusterInterface getClusterInterface () {

      return clusterInterface;
   }

}

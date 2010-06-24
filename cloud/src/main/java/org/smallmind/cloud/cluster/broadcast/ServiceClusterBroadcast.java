package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInterface;

public class ServiceClusterBroadcast extends ClusterBroadcast {

   private ClusterInterface clusterInterface;

   public ServiceClusterBroadcast (ClusterInterface clusterInterface)
      throws UnknownHostException {

      super(ClusterBroadcastType.SERVICE);

      this.clusterInterface = clusterInterface;
   }

   public ClusterInterface getClusterInterface () {

      return clusterInterface;
   }

}

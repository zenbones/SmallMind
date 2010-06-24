package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInstance;

public class ScatterShotClusterBroadcast extends SystemClusterBroadcast {

   ClusterInstance[] clusterInstances;

   public ScatterShotClusterBroadcast (ClusterInstance[] clusterInstances)
      throws UnknownHostException {

      super();

      this.clusterInstances = clusterInstances;
   }

   public ClusterInstance[] getClusterInstances () {

      return clusterInstances;
   }

}

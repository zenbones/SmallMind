package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInstance;

public class NodeOfflineClusterBroadcast extends SingleShotClusterBroadcast {

   public NodeOfflineClusterBroadcast (ClusterInstance clusterInstance)
      throws UnknownHostException {

      super(clusterInstance);
   }

}

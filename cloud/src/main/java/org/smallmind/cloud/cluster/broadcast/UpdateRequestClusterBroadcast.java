package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInterface;

public class UpdateRequestClusterBroadcast extends SemiAutomaticClusterBroadcast {

   public UpdateRequestClusterBroadcast (ClusterInterface clusterInterface)
      throws UnknownHostException {

      super(clusterInterface);
   }

}

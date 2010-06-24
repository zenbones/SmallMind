package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;

public class SystemClusterBroadcast extends ClusterBroadcast {

   public SystemClusterBroadcast ()
      throws UnknownHostException {

      super(ClusterBroadcastType.SYSTEM);
   }

}

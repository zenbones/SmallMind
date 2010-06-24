package org.smallmind.cloud.cluster.broadcast;

import java.net.UnknownHostException;
import org.smallmind.cloud.cluster.ClusterInstance;

public class UpdateResponseClusterBroadcast extends ScatterShotClusterBroadcast {

   private int calibratedFreeCapacity;

   public UpdateResponseClusterBroadcast (ClusterInstance[] clusterInstances, int calibratedFreeCapacity)
      throws UnknownHostException {

      super(clusterInstances);

      this.calibratedFreeCapacity = calibratedFreeCapacity;
   }

   public int getCalibratedFreeCapacity () {

      return calibratedFreeCapacity;
   }

}

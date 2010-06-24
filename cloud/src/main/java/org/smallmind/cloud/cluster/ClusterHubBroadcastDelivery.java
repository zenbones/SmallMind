package org.smallmind.cloud.cluster;

import org.smallmind.cloud.cluster.broadcast.ServiceClusterBroadcast;

public class ClusterHubBroadcastDelivery implements Runnable {

   private ClusterService clusterService;
   private ServiceClusterBroadcast serviceClusterBroadcast;

   public ClusterHubBroadcastDelivery (ClusterService clusterService, ServiceClusterBroadcast serviceClusterBroadcast) {

      this.clusterService = clusterService;
      this.serviceClusterBroadcast = serviceClusterBroadcast;
   }

   public void run () {

      clusterService.handleServiceBroadcast(serviceClusterBroadcast);
   }

}

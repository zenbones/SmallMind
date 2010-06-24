package org.smallmind.cloud.cluster.protocol.remote;

import java.util.HashMap;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagerBuilder;

public class RemoteClusterManagerBuilder implements ClusterManagerBuilder<RemoteClusterProtocolDetails> {

   private HashMap<String, RemoteClusterManager> managerMap;

   public RemoteClusterManagerBuilder () {

      managerMap = new HashMap<String, RemoteClusterManager>();
   }

   public synchronized RemoteClusterManager getClusterManager (ClusterHub clusterHub, ClusterInterface<RemoteClusterProtocolDetails> clusterInterface) {

      RemoteClusterManager clusterManager;

      if ((clusterManager = managerMap.get(clusterInterface.getClusterName())) == null) {
         clusterManager = new RemoteClusterManager(clusterHub, clusterInterface);
         managerMap.put(clusterInterface.getClusterName(), clusterManager);
      }

      return clusterManager;
   }

}
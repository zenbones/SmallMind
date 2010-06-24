package org.smallmind.cloud.cluster.protocol.socket;

import java.util.HashMap;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagerBuilder;

public class SocketClusterManagerBuilder implements ClusterManagerBuilder<SocketClusterProtocolDetails> {

   private HashMap<String, SocketClusterManager> managerMap;

   public SocketClusterManagerBuilder () {

      managerMap = new HashMap<String, SocketClusterManager>();
   }

   public synchronized SocketClusterManager getClusterManager (ClusterHub clusterHub, ClusterInterface<SocketClusterProtocolDetails> clusterInterface) {

      SocketClusterManager clusterManager;

      if ((clusterManager = managerMap.get(clusterInterface.getClusterName())) == null) {
         clusterManager = new SocketClusterManager(clusterHub, clusterInterface);
         managerMap.put(clusterInterface.getClusterName(), clusterManager);
      }

      return clusterManager;
   }

}
package org.smallmind.cloud.cluster.protocol.queue;

import java.util.HashMap;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInterface;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterManagerBuilder;

public class QueueClusterManagerBuilder implements ClusterManagerBuilder<QueueClusterProtocolDetails> {

   private HashMap<String, QueueClusterManager> managerMap;

   public QueueClusterManagerBuilder () {

      managerMap = new HashMap<String, QueueClusterManager>();
   }

   public QueueClusterManager getClusterManager (ClusterHub clusterHub, ClusterInterface<QueueClusterProtocolDetails> clusterInterface)
      throws ClusterManagementException {

      QueueClusterManager clusterManager;

      if ((clusterManager = managerMap.get(clusterInterface.getClusterName())) == null) {
         clusterManager = new QueueClusterManager(clusterHub, clusterInterface);
         managerMap.put(clusterInterface.getClusterName(), clusterManager);
      }

      return clusterManager;
   }

}

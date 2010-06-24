package org.smallmind.cloud.cluster;

import java.util.HashMap;
import org.smallmind.cloud.cluster.protocol.ClusterProtocol;

public class ClusterManagerFactory {

   private static final HashMap<ClusterProtocol, ClusterManagerBuilder> PROTOCOL_MAP = new HashMap<ClusterProtocol, ClusterManagerBuilder>();

   public static ClusterHandle getClusterHandle (ClusterHub clusterHub, ClusterInterface clusterInterface)
      throws ClusterManagementException {

      ClusterManager clusterManager;

      if ((clusterManager = clusterHub.getClusterManager(clusterInterface)) == null) {

         ClusterManagerBuilder clusterManagerBuilder;

         synchronized (PROTOCOL_MAP) {
            if ((clusterManagerBuilder = PROTOCOL_MAP.get(clusterInterface.getClusterProtocolDetails().getClusterProtocol())) == null) {
               try {
                  clusterManagerBuilder = clusterInterface.getClusterProtocolDetails().getClusterProtocol().getManagerBuilderClass().newInstance();
               }
               catch (Exception exception) {
                  throw new ClusterManagementException(exception);
               }

               PROTOCOL_MAP.put(clusterInterface.getClusterProtocolDetails().getClusterProtocol(), clusterManagerBuilder);
            }
         }

         clusterManager = clusterManagerBuilder.getClusterManager(clusterHub, clusterInterface);
         clusterHub.addClusterManager(clusterManager);
      }

      return clusterManager.getClusterHandle();
   }
}

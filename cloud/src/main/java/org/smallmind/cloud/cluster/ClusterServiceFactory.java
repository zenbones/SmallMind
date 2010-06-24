package org.smallmind.cloud.cluster;

import java.util.HashMap;
import org.smallmind.cloud.cluster.protocol.ClusterProtocol;

public class ClusterServiceFactory {

   private static final HashMap<ClusterProtocol, ClusterServiceBuilder> PROTOCOL_MAP = new HashMap<ClusterProtocol, ClusterServiceBuilder>();

   public static ClusterService getClusterService (ClusterHub clusterHub, ClusterInstance clusterInstance, ClusterMember clusterMember)
      throws ClusterManagementException {

      ClusterService clusterService;
      ClusterServiceBuilder clusterServiceBuilder;

      synchronized (PROTOCOL_MAP) {
         if ((clusterServiceBuilder = PROTOCOL_MAP.get(clusterInstance.getClusterInterface().getClusterProtocolDetails().getClusterProtocol())) == null) {
            try {
               clusterServiceBuilder = clusterInstance.getClusterInterface().getClusterProtocolDetails().getClusterProtocol().getServiceBuilderClass().newInstance();
            }
            catch (Exception exception) {
               throw new ClusterManagementException(exception);
            }

            PROTOCOL_MAP.put(clusterInstance.getClusterInterface().getClusterProtocolDetails().getClusterProtocol(), clusterServiceBuilder);
         }
      }

      clusterService = clusterServiceBuilder.getClusterService(clusterHub, clusterInstance);
      clusterService.bindClusterMember(clusterMember);

      return clusterService;
   }
}

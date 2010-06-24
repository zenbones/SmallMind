package org.smallmind.cloud.cluster.protocol.remote;

import java.rmi.RemoteException;
import org.smallmind.cloud.cluster.ClusterHub;
import org.smallmind.cloud.cluster.ClusterInstance;
import org.smallmind.cloud.cluster.ClusterManagementException;
import org.smallmind.cloud.cluster.ClusterServiceBuilder;

public class RemoteClusterServiceBuilder implements ClusterServiceBuilder<RemoteClusterProtocolDetails> {

   public RemoteClusterServiceImpl getClusterService (ClusterHub clusterHub, ClusterInstance<RemoteClusterProtocolDetails> clusterInstance)
      throws ClusterManagementException {

      try {
         return new RemoteClusterServiceImpl(clusterHub, clusterInstance);
      }
      catch (RemoteException remoteException) {
         throw new ClusterManagementException(remoteException);
      }
   }
}
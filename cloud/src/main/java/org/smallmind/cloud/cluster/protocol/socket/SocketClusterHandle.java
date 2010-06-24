package org.smallmind.cloud.cluster.protocol.socket;

import java.nio.channels.SocketChannel;
import org.smallmind.cloud.cluster.ClusterHandle;
import org.smallmind.cloud.cluster.ClusterManagementException;

public class SocketClusterHandle implements ClusterHandle {

   private SocketClusterManager socketClusterManager;

   public SocketClusterHandle (SocketClusterManager socketClusterManager) {

      this.socketClusterManager = socketClusterManager;
   }

   public SocketChannel connect (Object[] parameters)
      throws ClusterManagementException {

      return socketClusterManager.connect(parameters);
   }

}

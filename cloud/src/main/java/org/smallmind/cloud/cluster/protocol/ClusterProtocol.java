package org.smallmind.cloud.cluster.protocol;

import org.smallmind.cloud.cluster.ClusterManagerBuilder;
import org.smallmind.cloud.cluster.ClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.queue.QueueClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.queue.QueueClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.remote.RemoteClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.remote.RemoteClusterServiceBuilder;
import org.smallmind.cloud.cluster.protocol.socket.SocketClusterManagerBuilder;
import org.smallmind.cloud.cluster.protocol.socket.SocketClusterServiceBuilder;

public enum ClusterProtocol {

   REMOTE(RemoteClusterServiceBuilder.class, RemoteClusterManagerBuilder.class), QUEUE(QueueClusterServiceBuilder.class, QueueClusterManagerBuilder.class), SOCKET(SocketClusterServiceBuilder.class, SocketClusterManagerBuilder.class);

   private Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> serviceBuilderClass;
   private Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> managerBuilderClass;

   private ClusterProtocol (Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> serviceBuilderClass, Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> managerBuilderClass) {

      this.serviceBuilderClass = serviceBuilderClass;
      this.managerBuilderClass = managerBuilderClass;
   }

   public Class<? extends ClusterServiceBuilder<? extends ClusterProtocolDetails>> getServiceBuilderClass () {

      return serviceBuilderClass;
   }

   public Class<? extends ClusterManagerBuilder<? extends ClusterProtocolDetails>> getManagerBuilderClass () {

      return managerBuilderClass;
   }
}
